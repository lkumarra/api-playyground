package com.bestbuy.api.repository;

import com.bestbuy.api.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {

    @Override
    @EntityGraph(attributePaths = {"subCategories", "categoryPath"})
    Page<Category> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"subCategories", "categoryPath"})
    Optional<Category> findById(String id);
}
