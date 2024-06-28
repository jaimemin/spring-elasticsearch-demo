package com.tistory.jaimemin.indexer.batch;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.tistory.jaimemin.indexer.dto.ProductDto;
import com.tistory.jaimemin.indexer.entity.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CrawlJob {

	private static final int CHUNK_SIZE = 1000;

	private final JobBuilderFactory jobBuilderFactory;

	private final StepBuilderFactory stepBuilderFactory;

	private final EntityManagerFactory entityManagerFactory;

	@Bean
	public Job crawlJobBuild(Step crawlJobStep) {
		return jobBuilderFactory.get("crawlJob")
			.start(crawlJobStep)
			.build();
	}

	@Bean
	public Step crawlJobStep() {
		return stepBuilderFactory.get("crawlJobStep")
			.<ProductDto, Product>chunk(CHUNK_SIZE)
			.reader(crawlJobCsvItemReader())
			.processor(crawlJobCsvItemProcessor())
			.writer(crawlJobItemWriter())
			.build();
	}

	@Bean
	public FlatFileItemReader<ProductDto> crawlJobCsvItemReader() {
		return new FlatFileItemReaderBuilder<ProductDto>()
			.name("csvItemReader")
			.resource(new ClassPathResource("product_sample.csv"))
			.linesToSkip(1)
			.delimited()
			.names("asin", "title", "img_url", "product_url", "starts", "reviews", "price", "list_price", "category_id",
				"isBestSeller", "bought_in_last_month")
			.fieldSetMapper(new BeanWrapperFieldSetMapper<>() {
				{
					setTargetType(ProductDto.class);
				}
			})
			.build();
	}

	/**
	 * 별점 4.5 이상, 베스트셀러, 지난 달에 200개 이상 구매된 경우 추천 판매자로 지정
	 */
	@Bean
	public ItemProcessor<ProductDto, Product> crawlJobCsvItemProcessor() {
		return item -> convertItemToProduct(item);
	}

	private static Product convertItemToProduct(ProductDto item) {
		Product product = new Product();

		if (isRecommendSeller(item)) {
			product.setRecommendSeller(true);
		}

		product.setId(null);
		product.setAsin(item.getAsin());
		product.setTitle(item.getTitle());
		product.setImgUrl(item.getImgUrl());
		product.setProductUrl(item.getProductUrl());
		product.setStars(item.getStars());
		product.setReviews(item.getReviews());
		product.setPrice(item.getPrice());
		product.setListPrice(item.getListPrice());
		product.setCategoryId(item.getCategoryId());
		product.setBestSeller(item.isBestSeller());
		product.setBoughtInLastMonth(item.getBoughtInLastMonth());

		return product;
	}

	private static boolean isRecommendSeller(ProductDto item) {
		return item.getStars() >= 4.5
			&& item.isBestSeller()
			&& item.getBoughtInLastMonth() >= 200;
	}

	@Bean
	public JpaItemWriter<Product> crawlJobItemWriter() {
		JpaItemWriter<Product> jpaItemWriter = new JpaItemWriter<>();
		jpaItemWriter.setEntityManagerFactory(entityManagerFactory);

		return jpaItemWriter;
	}
}

