package com.bestbuy.api.repository;

import com.bestbuy.api.entity.InStoreService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InStoreServiceRepository extends JpaRepository<InStoreService, Long>, JpaSpecificationExecutor<InStoreService> {
}
