package com.bestbuy.api.service;

import com.bestbuy.api.dto.PaginatedResponse;
import com.bestbuy.api.dto.ServiceRequest;
import com.bestbuy.api.entity.InStoreService;
import com.bestbuy.api.repository.InStoreServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class InStoreServiceService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    private final InStoreServiceRepository repository;

    public InStoreServiceService(InStoreServiceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<InStoreService> findAll(Map<String, String> params) {
        int limit = parseLimit(params);
        int skip = parseSkip(params);
        PageRequest pageable = PageRequest.of(skip / Math.max(limit, 1), limit);

        Page<InStoreService> page = repository.findAll(pageable);

        return new PaginatedResponse<>(page.getContent(), page.getTotalElements(), limit, skip);
    }

    @Transactional(readOnly = true)
    public InStoreService findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No record found for id '" + id + "'"));
    }

    @Transactional
    public InStoreService create(ServiceRequest request) {
        InStoreService service = new InStoreService();
        service.setName(request.getName());
        return repository.save(service);
    }

    @Transactional
    public InStoreService patch(Long id, Map<String, Object> updates) {
        InStoreService service = findById(id);
        if (updates.containsKey("name")) {
            service.setName((String) updates.get("name"));
        }
        return repository.save(service);
    }

    @Transactional
    public InStoreService delete(Long id) {
        InStoreService service = findById(id);
        repository.delete(service);
        return service;
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
}
