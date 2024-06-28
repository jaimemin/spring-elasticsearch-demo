package com.tistory.jaimemin.searcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tistory.jaimemin.searcher.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

}
