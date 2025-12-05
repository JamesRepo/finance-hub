package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByCategoryType(CategoryType categoryType);
}