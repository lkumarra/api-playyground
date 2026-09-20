package com.bestbuy.api.repository;

import com.bestbuy.api.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long>, JpaSpecificationExecutor<Store> {

    @Override
    @EntityGraph(attributePaths = "services")
    Page<Store> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "services")
    Optional<Store> findById(Long id);

    @EntityGraph(attributePaths = "services")
    @Query("SELECT DISTINCT s FROM Store s JOIN s.services sv WHERE sv.id = :serviceId")
    Page<Store> findByServiceId(@Param("serviceId") Long serviceId, Pageable pageable);

    @EntityGraph(attributePaths = "services")
    @Query("SELECT DISTINCT s FROM Store s JOIN s.services sv WHERE sv.name = :serviceName")
    Page<Store> findByServiceName(@Param("serviceName") String serviceName, Pageable pageable);

    @EntityGraph(attributePaths = "services")
    @Query("SELECT s FROM Store s WHERE s.lat BETWEEN :southLat AND :northLat AND s.lng BETWEEN :westLng AND :eastLng")
    Page<Store> findNearby(
        @Param("southLat") BigDecimal southLat,
        @Param("northLat") BigDecimal northLat,
        @Param("westLng") BigDecimal westLng,
        @Param("eastLng") BigDecimal eastLng,
        Pageable pageable
    );
}
