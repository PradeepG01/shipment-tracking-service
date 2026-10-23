package com.pradeep.shipments.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /** Loads the shipment and its events in one query (avoids the N+1 select problem). */
    @EntityGraph(attributePaths = "events")
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    boolean existsByTrackingNumber(String trackingNumber);
}
