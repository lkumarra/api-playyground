package com.bestbuy.api.service;

import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.dto.ProductRequest;
import com.bestbuy.api.entity.Product;
import com.bestbuy.api.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProductService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<Product> findAll(Map<String, String> params) {
        int limit = parseLimit(params);
        int skip = parseSkip(params);
        PageRequest pageable = PageRequest.of(skip / Math.max(limit, 1), limit);

        String categoryId = extractParam(params, "category.id", "category[id]");
        String categoryName = extractParam(params, "category.name", "category[name]");

        Page<Product> page;
        if (categoryId != null) {
            page = productRepository.findByCategoryId(categoryId, pageable);
        } else if (categoryName != null) {
            page = productRepository.findByCategoryName(categoryName, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }

        return new PaginatedResponse<>(page.getContent(), page.getTotalElements(), limit, skip);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No record found for id '" + id + "'"));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product product = new Product();
        mapRequestToEntity(request, product);
        return productRepository.save(product);
    }

    @Transactional
    public Product patch(Long id, Map<String, Object> updates) {
        Product product = findById(id);
        applyPatch(product, updates);
        return productRepository.save(product);
    }

    @Transactional
    public Product delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
        return product;
    }

    private void mapRequestToEntity(ProductRequest req, Product product) {
        product.setName(req.getName());
        product.setType(req.getType());
        product.setPrice(req.getPrice());
        product.setUpc(req.getUpc());
        product.setShipping(req.getShipping());
        product.setDescription(req.getDescription());
        product.setManufacturer(req.getManufacturer());
        product.setModel(req.getModel());
        product.setUrl(req.getUrl());
        product.setImage(req.getImage());
    }

    private void applyPatch(Product product, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "name" -> product.setName((String) value);
                case "type" -> product.setType((String) value);
                case "price" -> product.setPrice(value != null ? new java.math.BigDecimal(value.toString()) : null);
                case "upc" -> product.setUpc((String) value);
                case "shipping" -> product.setShipping(value != null ? new java.math.BigDecimal(value.toString()) : null);
                case "description" -> product.setDescription((String) value);
                case "manufacturer" -> product.setManufacturer((String) value);
                case "model" -> product.setModel((String) value);
                case "url" -> product.setUrl((String) value);
                case "image" -> product.setImage((String) value);
            }
        });
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

    private String extractParam(Map<String, String> params, String dotKey, String bracketKey) {
        String value = params.get(dotKey);
        if (value == null) value = params.get(bracketKey);
        return value;
    }
}
