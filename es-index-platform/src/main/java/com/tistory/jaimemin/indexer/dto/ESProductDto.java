package com.tistory.jaimemin.indexer.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ESProductDto {

	private Long id;

	private String asin;

	private String title;

	private String imgUrl;

	private String productUrl;

	private float stars;

	private int reviews;

	private BigDecimal price;

	private BigDecimal listPrice;

	private int categoryId;

	@JsonProperty("is_best_seller")
	private boolean isBestSeller;

	private int boughtInLastMonth;

	@JsonProperty("is_recommend_seller")
	private boolean isRecommendSeller;

	private Date createdAt;

	private Date updatedAt;
}
