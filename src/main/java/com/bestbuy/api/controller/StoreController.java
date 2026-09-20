package com.bestbuy.api.controller;

import com.bestbuy.api.dto.ErrorResponse;
import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.dto.StoreRequest;
import com.bestbuy.api.entity.Store;
import com.bestbuy.api.service.StoreService;
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
@RequestMapping("/stores")
@Tag(name = "Stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    @Operation(
        summary = "Get all stores",
        description = """
            Returns a paginated list of stores. Supports geolocation search using \
            the 'near' parameter with a US zip code, and optional 'miles' radius (default 10). \
            Also supports filtering by service ID or name.""",
        parameters = {
            @Parameter(name = "$limit", description = "Number of results to return (default 10, max 25)", example = "10"),
            @Parameter(name = "$skip", description = "Number of results to skip for pagination", example = "0"),
            @Parameter(name = "near", description = "US zip code for geolocation search", example = "55401"),
            @Parameter(name = "miles", description = "Radius in miles for geolocation search (default 10)", example = "15"),
            @Parameter(name = "service.id", description = "Filter by service ID", example = "4"),
            @Parameter(name = "service.name", description = "Filter by service name", example = "Geek Squad Services"),
            @Parameter(name = "city", description = "Filter by city"),
            @Parameter(name = "state", description = "Filter by state"),
            @Parameter(name = "zip", description = "Filter by zip code")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated list of stores")
        }
    )
    public PaginatedResponse<Store> findAll(@RequestParam Map<String, String> params) {
        return storeService.findAll(params);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a store by ID",
        description = "Returns a single store with its associated in-store services.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Store found"),
            @ApiResponse(responseCode = "404", description = "Store not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public Store findById(@PathVariable Long id) {
        return storeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new store",
        description = "Creates a new store. Required fields: name, address, city, state, zip.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Store created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "name": "New Store",
                  "type": "BigBox",
                  "address": "123 Main St",
                  "city": "Minneapolis",
                  "state": "MN",
                  "zip": "55401",
                  "lat": 44.9778,
                  "lng": -93.2650,
                  "hours": "Mon-Sat: 10-9; Sun: 10-6"
                }"""))
        )
    )
    public Store create(@Valid @RequestBody StoreRequest request) {
        return storeService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Partially update a store",
        description = "Updates only the provided fields of an existing store.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Store updated"),
            @ApiResponse(responseCode = "404", description = "Store not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "hours": "Mon-Sat: 9-10; Sun: 10-7"
                }"""))
        )
    )
    public Store patch(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return storeService.patch(id, updates);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a store",
        description = "Deletes a store and returns the deleted object.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Store deleted"),
            @ApiResponse(responseCode = "404", description = "Store not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public Store delete(@PathVariable Long id) {
        return storeService.delete(id);
    }
}
