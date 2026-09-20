package com.bestbuy.api.service;

import com.bestbuy.api.dto.CategoryRequest;
import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.entity.Category;
import com.bestbuy.api.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<Category> findAll(Map<String, String> params) {
        int limit = parseLimit(params);
        int skip = parseSkip(params);
        PageRequest pageable = PageRequest.of(skip / Math.max(limit, 1), limit);

        Page<Category> page = categoryRepository.findAll(pageable);

        List<Category> filtered = page.getContent().stream()
            .map(this::filterParentReferences)
            .toList();

        return new PaginatedResponse<>(filtered, page.getTotalElements(), limit, skip);
    }

    @Transactional(readOnly = true)
    public Category findById(String id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No record found for id '" + id + "'"));
        return filterParentReferences(category);
    }

    @Transactional
    public Category create(CategoryRequest request) {
        Category category = new Category();
        category.setId(request.getId());
        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    @Transactional
    public Category patch(String id, Map<String, Object> updates) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No record found for id '" + id + "'"));
        if (updates.containsKey("name")) {
            category.setName((String) updates.get("name"));
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category delete(String id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No record found for id '" + id + "'"));
        categoryRepository.delete(category);
        return category;
    }

    private Category filterParentReferences(Category category) {
        // Prevent infinite recursion by clearing nested self-references
        if (category.getSubCategories() != null) {
            for (Category sub : category.getSubCategories()) {
                sub.setSubCategories(new LinkedHashSet<>());
                sub.setCategoryPath(new LinkedHashSet<>());
            }
        }
        if (category.getCategoryPath() != null) {
            for (Category path : category.getCategoryPath()) {
                path.setSubCategories(new LinkedHashSet<>());
                path.setCategoryPath(new LinkedHashSet<>());
            }
        }
        return category;
    }

    private int parseLimit(Map<String, String> params) {
        String val = params.get("$limit");
        if (val == null) return DEFAULT_LIMIT;
        int limit = Integer.parseInt(val);
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private int parseSkip(Map<String, String> params) {
        String val = params.get("$skip");
        if (val == null) return 0;
        return Math.max(Integer.parseInt(val), 0);
    }
}
