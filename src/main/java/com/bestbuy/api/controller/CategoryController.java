package com.bestbuy.api.controller;

import com.bestbuy.api.dto.CategoryRequest;
import com.bestbuy.api.dto.ErrorResponse;
import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.entity.Category;
import com.bestbuy.api.service.CategoryService;
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
@RequestMapping("/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(
        summary = "Get all categories",
        description = "Returns a paginated list of categories. Each category includes its subcategories and category path hierarchy.",
        parameters = {
            @Parameter(name = "$limit", description = "Number of results to return (default 10, max 25)", example = "10"),
            @Parameter(name = "$skip", description = "Number of results to skip for pagination", example = "0"),
            @Parameter(name = "name", description = "Filter by category name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated list of categories")
        }
    )
    public PaginatedResponse<Category> findAll(@RequestParam Map<String, String> params) {
        return categoryService.findAll(params);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a category by ID",
        description = "Returns a single category with its subcategories and category path hierarchy.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public Category findById(@PathVariable @Parameter(description = "Category ID", example = "abcat0010000") String id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new category",
        description = "Creates a new category. Both id and name are required.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "id": "custom-cat-001",
                  "name": "Custom Category"
                }"""))
        )
    )
    public Category create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Partially update a category",
        description = "Updates only the provided fields of an existing category.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "404", description = "Category not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "name": "Updated Category Name"
                }"""))
        )
    )
    public Category patch(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return categoryService.patch(id, updates);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a category",
        description = "Deletes a category and returns the deleted object.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Category deleted"),
            @ApiResponse(responseCode = "404", description = "Category not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public Category delete(@PathVariable String id) {
        return categoryService.delete(id);
    }
}
