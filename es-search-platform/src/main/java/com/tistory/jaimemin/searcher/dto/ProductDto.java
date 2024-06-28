package com.tistory.jaimemin.searcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {

	private String asin;

	private String title;

	private String imgUrl;

	private String productUrl;

	private double stars;

	private int reviews;

	private double price;

	private double list_price;

	@JsonIgnore
	private int categoryId;

	private String categoryName;

	private double score;

	private String createdAt;

	private String updatedAt;
}
