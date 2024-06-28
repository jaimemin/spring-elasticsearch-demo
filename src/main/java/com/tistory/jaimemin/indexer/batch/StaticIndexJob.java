package com.tistory.jaimemin.indexer.batch;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.tistory.jaimemin.indexer.dto.ESProductDto;
import com.tistory.jaimemin.indexer.entity.Product;
import com.tistory.jaimemin.indexer.helper.ESHelper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StaticIndexJob {

	private static final int PAGE_SIZE = 1000;

	private static final int CHUNK_SIZE = 1000;

	private static final int POOL_SIZE = 10;

	private final JobBuilderFactory jobBuilderFactory;

	private final StepBuilderFactory stepBuilderFactory;

	private final EntityManagerFactory entityManagerFactory;

	private final ElasticsearchClient elasticsearchClient;

	private Date lastUpdatedAt;

	@Bean
	public Job staticIndexJobBuild(Step staticIndexJobStep
		, Step staticIndexJobStep2
		, Step staticIndexJobStep3) {
		return jobBuilderFactory.get("staticIndexJob")
			.start(staticIndexJobStep)
			.next(staticIndexJobStep2)
			.next(staticIndexJobStep3)
			.build();
	}

	@Bean
	public Step staticIndexJobStep(ItemWriter<Product> staticIndexJobElasticBulkWriter) {
		log.debug("staticIndexJobStep start");
		lastUpdatedAt = new Date();

		return stepBuilderFactory.get("staticIndexJobStep")
			.<Product, Product>chunk(CHUNK_SIZE)
			.reader(fullItemReader())
			.writer(staticIndexJobElasticBulkWriter)
			.taskExecutor(executor()) // 성능 향상을 위한 병렬 처리
			.throttleLimit(POOL_SIZE)
			.build();
	}

	@Bean
	public TaskExecutor executor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(POOL_SIZE);
		executor.setMaxPoolSize(POOL_SIZE);
		executor.setThreadNamePrefix("multi-thread-");
		executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
		executor.initialize();

		return executor;
	}

	@Bean
	public Step staticIndexJobStep2(ItemWriter<Product> staticIndexJobElasticBulkWriter) {
		log.debug("staticIndexJobStep2 start");

		return stepBuilderFactory.get("staticIndexJobStep2")
			.<Product, Product>chunk(CHUNK_SIZE)
			.reader(staticIndexJobIncrementProductsReader())
			.writer(staticIndexJobElasticBulkWriter)
			.build();
	}

	@Bean
	@JobScope
	public Step staticIndexJobStep3(@Value("#{jobParameters[date]}") String date) {
		log.debug("staticIndexJobStep3 start");

		return stepBuilderFactory.get("staticIndexJobStep3")
			.tasklet((stepContribution, chunkContext) -> {
				changeElasticsearchAlias(date);

				return RepeatStatus.FINISHED;
			}).build();
	}

	@Bean
	public JpaPagingItemReader<Product> fullItemReader() {
		return new JpaPagingItemReader<>() {{
			setEntityManagerFactory(entityManagerFactory);
			setQueryString("SELECT p FROM Product p");
			setPageSize(PAGE_SIZE);
		}};
	}

	@Bean
	public JpaPagingItemReader<Product> staticIndexJobIncrementProductsReader() {
		return new JpaPagingItemReader<>() {{
			setEntityManagerFactory(entityManagerFactory);
			setQueryString("SELECT p FROM Product p WHERE p.updatedAt > :lastUpdatedAt");
			setParameterValues(Collections.singletonMap("lastUpdatedAt", lastUpdatedAt));
			setPageSize(PAGE_SIZE);
		}};
	}

	@Bean
	@JobScope
	@StepScope
	public ItemWriter<Product> staticIndexJobElasticBulkWriter(@Value("#{jobParameters[date]}") String date
		, ElasticsearchClient elasticsearchClient) {
		return products -> {
			log.info("writing products: {}", products);

			BulkRequest.Builder builder = new BulkRequest.Builder();

			for (Product product : products) {
				ESProductDto esProductDto = convertProductToEsProductDto(product);
				builder.operations(op ->
					op.index(idx ->
						idx.index("products_" + date)
							.id("product_" + product.getId())
							.document(esProductDto)));
			}

			ESHelper.doBulk(elasticsearchClient, builder);
		};
	}

	private static ESProductDto convertProductToEsProductDto(Product product) {
		ESProductDto esProductDto = new ESProductDto();
		esProductDto.setId(product.getId());
		esProductDto.setAsin(product.getAsin());
		esProductDto.setTitle(product.getTitle());
		esProductDto.setImgUrl(product.getImgUrl());
		esProductDto.setProductUrl(product.getProductUrl());
		esProductDto.setStars(product.getStars());
		esProductDto.setReviews(product.getReviews());
		esProductDto.setPrice(product.getPrice());
		esProductDto.setListPrice(product.getListPrice());
		esProductDto.setCategoryId(product.getCategoryId());
		esProductDto.setBestSeller(product.isBestSeller());
		esProductDto.setBoughtInLastMonth(product.getBoughtInLastMonth());
		esProductDto.setRecommendSeller(product.isRecommendSeller());
		esProductDto.setCreatedAt(product.getCreatedAt());
		esProductDto.setUpdatedAt(product.getUpdatedAt());

		return esProductDto;
	}

	private void changeElasticsearchAlias(String date) throws IOException {
		GetAliasRequest request = GetAliasRequest.of(b -> b.name("products"));
		GetAliasResponse response = elasticsearchClient.indices().getAlias(request);
		String oldIndex = response.result().keySet().iterator().next();
		String newIndexName = "products_" + date;

		log.info("alias change '{}' -> '{}'", oldIndex, newIndexName);

		UpdateAliasesRequest.Builder builder = new UpdateAliasesRequest.Builder();
		builder.actions(actions ->
			actions.remove(remove ->
				remove.index(oldIndex)
					.alias("products")));
		builder.actions(actions ->
			actions.add(add ->
				add.index(newIndexName)
					.alias("products")));
		UpdateAliasesResponse updateAliasesResponse = elasticsearchClient.indices()
			.updateAliases(builder.build());

		if (updateAliasesResponse.acknowledged()) {
			log.info("'products' alias가 성공적으로 교체되었습니다.");

			removeOldIndex(oldIndex);
		} else {
			log.error("alias 교체 실패");
		}
	}

	private void removeOldIndex(String oldIndex) throws IOException {
		DeleteIndexRequest request = DeleteIndexRequest.of(b -> b.index(oldIndex));
		DeleteIndexResponse response = elasticsearchClient.indices().delete(request);

		if (response.acknowledged()) {
			log.info("Old index '{}'가 성공적으로 삭제되었습니다.", oldIndex);
		} else {
			log.error("Old index '{}' 삭제 실패", oldIndex);
		}
	}
}