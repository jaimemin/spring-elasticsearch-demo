package com.tistory.jaimemin.indexer.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

	private String asin;

	private String title;

	private String imgUrl;

	private String productUrl;

	private float stars;

	private int reviews;

	private BigDecimal price;

	private BigDecimal listPrice;

	private int categoryId;

	private boolean isBestSeller;

	private int boughtInLastMonth;
}
