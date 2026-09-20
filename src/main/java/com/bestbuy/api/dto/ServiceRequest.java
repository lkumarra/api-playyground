package com.bestbuy.api.dto;

import jakarta.validation.constraints.*;

public class ServiceRequest {

    @NotBlank @Size(min = 1, max = 100)
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
