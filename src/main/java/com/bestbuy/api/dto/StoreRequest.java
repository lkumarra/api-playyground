package com.bestbuy.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class StoreRequest {

    @NotBlank @Size(min = 1, max = 100)
    private String name;

    @Size(min = 1, max = 30)
    private String type;

    @NotBlank @Size(min = 1, max = 50)
    private String address;

    @Size(max = 30)
    private String address2;

    @NotBlank @Size(min = 1, max = 50)
    private String city;

    @NotBlank @Size(min = 1, max = 30)
    private String state;

    @NotBlank @Size(min = 1, max = 30)
    private String zip;

    private BigDecimal lat;
    private BigDecimal lng;

    @Size(min = 1, max = 100)
    private String hours;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public String getHours() { return hours; }
    public void setHours(String hours) { this.hours = hours; }
}
