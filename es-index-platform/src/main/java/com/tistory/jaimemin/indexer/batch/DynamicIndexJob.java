package com.tistory.jaimemin.indexer.batch;

import java.util.Collections;
import java.util.Date;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tistory.jaimemin.indexer.dto.ESProductDto;
import com.tistory.jaimemin.indexer.entity.Product;
import com.tistory.jaimemin.indexer.helper.ESHelper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicIndexJob {

	private static final int PAGE_SIZE = 1000;

	private static final int CHUNK_SIZE = 1000;

	private final JobBuilderFactory jobBuilderFactory;

	private final StepBuilderFactory stepBuilderFactory;

	private final EntityManagerFactory entityManagerFactory;

	private final ElasticsearchClient elasticsearchClient;

	@Bean
	public Job dynamicIndexJobBuild(Step dynamicIndexJobStep) {
		return jobBuilderFactory.get("dynamicIndexJob")
			.start(dynamicIndexJobStep)
			.incrementer(new RunIdIncrementer())
			.build();
	}

	@Bean
	public Step dynamicIndexJobStep() {
		log.info("dynamicIndexJobStep start");

		return stepBuilderFactory.get("dynamicIndexJobStep")
			.<Product, Product>chunk(CHUNK_SIZE)
			.reader(incrementProductsReader())
			.writer(dynamicIndexJobElasticBulkWriter())
			.build();
	}

	@Bean
	public JpaPagingItemReader<Product> incrementProductsReader() {
		log.info("incrementProductsReader start");

		return new JpaPagingItemReader<>() {{
			setEntityManagerFactory(entityManagerFactory);
			setQueryString("SELECT p FROM Product p WHERE p.updatedAt > :lastUpdatedAt");

			Date lastUpdatedAt = new Date(System.currentTimeMillis() - (60 * 1000));
			setParameterValues(Collections.singletonMap("lastUpdatedAt", lastUpdatedAt));
			setPageSize(PAGE_SIZE);
		}};
	}

	@Bean
	@StepScope
	public ItemWriter<Product> dynamicIndexJobElasticBulkWriter() {
		return products -> {
			GetAliasRequest request = GetAliasRequest.of(b -> b.name("products"));
			GetAliasResponse response = elasticsearchClient.indices().getAlias(request);
			String currentIndex = response.result().keySet().iterator().next();

			log.info("writing products: {}", products);

			BulkRequest.Builder builder = new BulkRequest.Builder();

			for (Product product : products) {
				ESProductDto esProductDto = convertProductToEsProductDto(product);
				builder.operations(op ->
					op.index(idx ->
						idx.index(currentIndex)
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
}

