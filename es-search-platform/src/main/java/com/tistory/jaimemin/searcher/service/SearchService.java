package com.tistory.jaimemin.searcher.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tistory.jaimemin.searcher.dto.ProductDto;
import com.tistory.jaimemin.searcher.entity.Category;
import com.tistory.jaimemin.searcher.entity.Product;
import com.tistory.jaimemin.searcher.repository.CategoryRepository;
import com.tistory.jaimemin.searcher.repository.ElasticsearchRepository;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private final CategoryRepository categoryRepository;

	private final ElasticsearchRepository elasticsearchRepository;

	public List<ProductDto> searchProducts(String query, int size, Double lastScore, String lastAsin) throws
		IOException {
		SearchResponse<Product> searchResponse = elasticsearchRepository.searchProducts(query, size, lastScore,
			lastAsin);

		return convertToProductDtos(searchResponse);
	}

	private List<ProductDto> convertToProductDtos(SearchResponse<Product> searchResponse) {
		Map<Long, String> id2name = initCategoryHashMap();
		List<ProductDto> productDtos = new ArrayList<>();

		for (Hit<Product> hit : searchResponse.hits().hits()) {
			Product source = hit.source();

			productDtos.add(convertToProductDto(hit, source, id2name));
		}

		return productDtos;
	}

	private Map<Long, String> initCategoryHashMap() {
		List<Category> categories = categoryRepository.findAll();
		Map<Long, String> id2name = new HashMap<>();

		for (Category category : categories) {
			id2name.put(category.getId(), category.getCategoryName());
		}

		return id2name;
	}

	private ProductDto convertToProductDto(Hit<Product> hit, Product source, Map<Long, String> id2name) {
		ProductDto productDto = new ProductDto();
		productDto.setAsin(source.getAsin());
		productDto.setTitle(source.getTitle());
		productDto.setProductUrl(source.getProductUrl());
		productDto.setImgUrl(source.getImgUrl());
		productDto.setReviews(source.getReviews());
		productDto.setPrice(source.getPrice().doubleValue());
		productDto.setStars(source.getStars());
		productDto.setCategoryId(source.getCategoryId());
		productDto.setCategoryName(id2name.get(source.getCategoryId() * 1L));
		productDto.setCreatedAt(dateFormatter.format(source.getCreatedAt()));
		productDto.setUpdatedAt(dateFormatter.format(source.getUpdatedAt()));

		Double score = hit.score();

		if (score != null) {
			productDto.setScore(score);
		}

		return productDto;
	}
}