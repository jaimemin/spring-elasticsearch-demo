package com.tistory.jaimemin.indexer.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product {

	@javax.persistence.Id
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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

	private boolean isBestSeller;

	private int boughtInLastMonth;

	private boolean isRecommendSeller;

	@CreationTimestamp
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;
}
