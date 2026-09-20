package com.bestbuy.api.service;

import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.dto.StoreRequest;
import com.bestbuy.api.entity.Store;
import com.bestbuy.api.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class StoreService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    private final StoreRepository storeRepository;
    private final GeoLocationService geoLocationService;

    public StoreService(StoreRepository storeRepository, GeoLocationService geoLocationService) {
        this.storeRepository = storeRepository;
        this.geoLocationService = geoLocationService;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<Store> findAll(Map<String, String> params) {
        int limit = parseLimit(params);
        int skip = parseSkip(params);
        PageRequest pageable = PageRequest.of(skip / Math.max(limit, 1), limit);

        String near = params.get("near");
        String serviceId = extractParam(params, "service.id", "service[id]");
        String serviceName = extractParam(params, "service.name", "service[name]");

        Page<Store> page;
        if (near != null) {
            double miles = params.containsKey("miles") ? Double.parseDouble(params.get("miles")) : 10;
            GeoLocationService.BoundingBox box = geoLocationService.calculateBoundingBox(near, miles);
            page = storeRepository.findNearby(box.southLat(), box.northLat(), box.westLng(), box.eastLng(), pageable);
        } else if (serviceId != null) {
            page = storeRepository.findByServiceId(Long.parseLong(serviceId), pageable);
        } else if (serviceName != null) {
            page = storeRepository.findByServiceName(serviceName, pageable);
        } else {
            page = storeRepository.findAll(pageable);
        }

        return new PaginatedResponse<>(page.getContent(), page.getTotalElements(), limit, skip);
    }

    @Transactional(readOnly = true)
    public Store findById(Long id) {
        return storeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No record found for id '" + id + "'"));
    }

    @Transactional
    public Store create(StoreRequest request) {
        Store store = new Store();
        mapRequestToEntity(request, store);
        return storeRepository.save(store);
    }

    @Transactional
    public Store patch(Long id, Map<String, Object> updates) {
        Store store = findById(id);
        applyPatch(store, updates);
        return storeRepository.save(store);
    }

    @Transactional
    public Store delete(Long id) {
        Store store = findById(id);
        storeRepository.delete(store);
        return store;
    }

    private void mapRequestToEntity(StoreRequest req, Store store) {
        store.setName(req.getName());
        store.setType(req.getType());
        store.setAddress(req.getAddress());
        store.setAddress2(req.getAddress2());
        store.setCity(req.getCity());
        store.setState(req.getState());
        store.setZip(req.getZip());
        store.setLat(req.getLat());
        store.setLng(req.getLng());
        store.setHours(req.getHours());
    }

    private void applyPatch(Store store, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "name" -> store.setName((String) value);
                case "type" -> store.setType((String) value);
                case "address" -> store.setAddress((String) value);
                case "address2" -> store.setAddress2((String) value);
                case "city" -> store.setCity((String) value);
                case "state" -> store.setState((String) value);
                case "zip" -> store.setZip((String) value);
                case "lat" -> store.setLat(value != null ? new BigDecimal(value.toString()) : null);
                case "lng" -> store.setLng(value != null ? new BigDecimal(value.toString()) : null);
                case "hours" -> store.setHours((String) value);
            }
        });
    }

    private int parseLimit(Map<String, String> params) {
        String val = params.get("$limit");
        if (val == null) return DEFAULT_LIMIT;
        int limit = Integer.parseInt(val);
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private int parseSkip(Map<String, String> params) {
        String val = params.get("$skip");
        if (val == null) return 0;
        return Math.max(Integer.parseInt(val), 0);
    }

    private String extractParam(Map<String, String> params, String dotKey, String bracketKey) {
        String value = params.get(dotKey);
        if (value == null) value = params.get(bracketKey);
        return value;
    }
}
