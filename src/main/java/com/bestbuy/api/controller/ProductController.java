package com.bestbuy.api.controller;

import com.bestbuy.api.dto.ErrorResponse;
import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.dto.ProductRequest;
import com.bestbuy.api.entity.Product;
import com.bestbuy.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(
        summary = "Get all products",
        description = "Returns a paginated list of products. Supports filtering by category ID or name.",
        parameters = {
            @Parameter(name = "$limit", description = "Number of results to return (default 10, max 25)", example = "10"),
            @Parameter(name = "$skip", description = "Number of results to skip for pagination", example = "0"),
            @Parameter(name = "category.id", description = "Filter by category ID", example = "abcat0101000"),
            @Parameter(name = "category.name", description = "Filter by category name", example = "TVs"),
            @Parameter(name = "name", description = "Filter by product name"),
            @Parameter(name = "type", description = "Filter by product type"),
            @Parameter(name = "manufacturer", description = "Filter by manufacturer")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated list of products")
        }
    )
    public PaginatedResponse<Product> findAll(@RequestParam Map<String, String> params) {
        return productService.findAll(params);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a product by ID",
        description = "Returns a single product with its associated categories.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public Product findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new product",
        description = "Creates a new product. Required fields: name, type, upc, description, model.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "name": "New Product",
                  "type": "Electronics",
                  "upc": "123456789",
                  "price": 99.99,
                  "shipping": 5.99,
                  "description": "A great product",
                  "manufacturer": "Acme Corp",
                  "model": "NP-100"
                }"""))
        )
    )
    public Product create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Partially update a product",
        description = "Updates only the provided fields of an existing product.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "price": 79.99,
                  "name": "Updated Product Name"
                }"""))
        )
    )
    public Product patch(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return productService.patch(id, updates);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a product",
        description = "Deletes a product and returns the deleted object.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public Product delete(@PathVariable Long id) {
        return productService.delete(id);
    }
}
