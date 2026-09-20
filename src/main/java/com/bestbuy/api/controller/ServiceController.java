package com.bestbuy.api.controller;

import com.bestbuy.api.dto.ErrorResponse;
import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.dto.ServiceRequest;
import com.bestbuy.api.entity.InStoreService;
import com.bestbuy.api.service.InStoreServiceService;
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
@RequestMapping("/services")
@Tag(name = "Services")
public class ServiceController {

    private final InStoreServiceService serviceService;

    public ServiceController(InStoreServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    @Operation(
        summary = "Get all services",
        description = "Returns a paginated list of in-store services offered at Best Buy locations.",
        parameters = {
            @Parameter(name = "$limit", description = "Number of results to return (default 10, max 25)", example = "10"),
            @Parameter(name = "$skip", description = "Number of results to skip for pagination", example = "0"),
            @Parameter(name = "name", description = "Filter by service name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated list of services")
        }
    )
    public PaginatedResponse<InStoreService> findAll(@RequestParam Map<String, String> params) {
        return serviceService.findAll(params);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a service by ID",
        description = "Returns a single in-store service.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Service found"),
            @ApiResponse(responseCode = "404", description = "Service not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public InStoreService findById(@PathVariable Long id) {
        return serviceService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new service",
        description = "Creates a new in-store service. Only 'name' is required.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Service created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "name": "New Service"
                }"""))
        )
    )
    public InStoreService create(@Valid @RequestBody ServiceRequest request) {
        return serviceService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Partially update a service",
        description = "Updates only the provided fields of an existing service.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Service updated"),
            @ApiResponse(responseCode = "404", description = "Service not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                  "name": "Updated Service Name"
                }"""))
        )
    )
    public InStoreService patch(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return serviceService.patch(id, updates);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a service",
        description = "Deletes a service and returns the deleted object.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Service deleted"),
            @ApiResponse(responseCode = "404", description = "Service not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = "Read-only mode enabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public InStoreService delete(@PathVariable Long id) {
        return serviceService.delete(id);
    }
}
