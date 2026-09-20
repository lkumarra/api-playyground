package com.bestbuy.api.dto;

import jakarta.validation.constraints.*;

public class CategoryRequest {

    @NotBlank @Size(min = 1, max = 100)
    private String id;

    @NotBlank @Size(min = 1, max = 100)
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
