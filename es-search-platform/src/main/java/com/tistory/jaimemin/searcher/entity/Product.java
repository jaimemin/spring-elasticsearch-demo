package com.tistory.jaimemin.searcher.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("asin")
	private String asin;

	@JsonProperty("title")
	private String title;

	@JsonProperty("img_url")
	private String imgUrl;

	@JsonProperty("product_url")
	private String productUrl;

	@JsonProperty("stars")
	private float stars;

	@JsonProperty("reviews")
	private int reviews;

	@JsonProperty("price")
	private BigDecimal price;

	@JsonProperty("list_price")
	private BigDecimal listPrice;

	@JsonProperty("category_id")
	private int categoryId;

	@JsonProperty("bought_in_last_month")
	private int boughtInLastMonth;

	@JsonProperty("is_best_seller")
	private boolean isBestSeller;

	@JsonProperty("is_recommend_seller")
	private boolean isRecommendSeller;

	@JsonProperty("created_at")
	private Timestamp createdAt;

	@JsonProperty("updated_at")
	private Timestamp updatedAt;
}