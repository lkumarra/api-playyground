package com.bestbuy.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank @Size(min = 1, max = 100)
    private String name;

    @NotBlank @Size(min = 1, max = 30)
    private String type;

    @DecimalMin("0.00")
    private BigDecimal price;

    @NotBlank @Size(min = 1, max = 15)
    private String upc;

    @DecimalMin("0.00")
    private BigDecimal shipping;

    @NotBlank @Size(min = 1, max = 100)
    private String description;

    @Size(min = 1, max = 50)
    private String manufacturer;

    @NotBlank @Size(min = 1, max = 25)
    private String model;

    @Size(min = 1, max = 500)
    private String url;

    @Size(min = 1, max = 500)
    private String image;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getUpc() { return upc; }
    public void setUpc(String upc) { this.upc = upc; }

    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
