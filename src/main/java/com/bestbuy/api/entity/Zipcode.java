package com.bestbuy.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "zipcodes")
public class Zipcode {

    @Id
    private String zip;

    private BigDecimal lat;
    private BigDecimal lng;
    private String city;
    private String state;

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
