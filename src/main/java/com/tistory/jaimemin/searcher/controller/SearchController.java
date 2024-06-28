package com.tistory.jaimemin.searcher.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tistory.jaimemin.searcher.dto.ProductDto;
import com.tistory.jaimemin.searcher.service.SearchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	@GetMapping("api/products")
	public List<ProductDto> getProducts(
		@RequestParam(value = "size", required = false, defaultValue = "10") int size,
		@RequestParam(value = "query") String query,
		@RequestParam(value = "last_asin", required = false) String lastAsin,
		@RequestParam(value = "last_score", required = false) Double lastScore
	) throws IOException {
		List<ProductDto> productDtos = searchService.searchProducts(query, size, lastScore, lastAsin);

		return productDtos;
	}

}
