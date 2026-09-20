package com.bestbuy.api.service;

import com.bestbuy.api.entity.Zipcode;
import com.bestbuy.api.repository.ZipcodeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GeoLocationService {

    private static final double EARTH_RADIUS = 6_371_000; // meters
    private static final double MILES_TO_METERS = 1609;

    private final ZipcodeRepository zipcodeRepository;

    public GeoLocationService(ZipcodeRepository zipcodeRepository) {
        this.zipcodeRepository = zipcodeRepository;
    }

    public BoundingBox calculateBoundingBox(String zipCode, double miles) {
        Zipcode zip = zipcodeRepository.findById(zipCode)
            .orElseThrow(() -> new IllegalArgumentException("Zipcode not found: " + zipCode));

        double lat = zip.getLat().doubleValue();
        double lng = zip.getLng().doubleValue();
        double radiusInMeters = miles * MILES_TO_METERS;

        double north = calcDerivedLatitude(lat, radiusInMeters, 0);
        double[] east = calcDerivedPosition(lat, lng, radiusInMeters, 90);
        double south = calcDerivedLatitude(lat, radiusInMeters, 180);
        double[] west = calcDerivedPosition(lat, lng, radiusInMeters, 270);

        return new BoundingBox(
            BigDecimal.valueOf(south),
            BigDecimal.valueOf(north),
            BigDecimal.valueOf(west[1]),
            BigDecimal.valueOf(east[1])
        );
    }

    private double[] calcDerivedPosition(double lat, double lng, double range, double bearing) {
        double latA = Math.toRadians(lat);
        double lngA = Math.toRadians(lng);
        double angularDistance = range / EARTH_RADIUS;
        double trueCourse = Math.toRadians(bearing);

        double destLat = Math.asin(
            Math.sin(latA) * Math.cos(angularDistance) +
            Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse)
        );

        double destDLng = Math.atan2(
            Math.sin(trueCourse) * Math.sin(angularDistance) * Math.cos(latA),
            Math.cos(angularDistance) - Math.sin(latA) * Math.sin(Math.toRadians(lat))
        );

        double destLng = ((lngA + destDLng + Math.PI) % (Math.PI * 2)) - Math.PI;

        return new double[]{Math.toDegrees(destLat), Math.toDegrees(destLng)};
    }

    private double calcDerivedLatitude(double lat, double range, double bearing) {
        double latA = Math.toRadians(lat);
        double angularDistance = range / EARTH_RADIUS;
        double trueCourse = Math.toRadians(bearing);

        double destLat = Math.asin(
            Math.sin(latA) * Math.cos(angularDistance) +
            Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse)
        );

        return Math.toDegrees(destLat);
    }

    public record BoundingBox(BigDecimal southLat, BigDecimal northLat, BigDecimal westLng, BigDecimal eastLng) {}
}
