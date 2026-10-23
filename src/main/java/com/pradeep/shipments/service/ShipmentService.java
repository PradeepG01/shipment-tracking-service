package com.pradeep.shipments.service;

import com.pradeep.shipments.domain.Shipment;
import com.pradeep.shipments.domain.ShipmentRepository;
import com.pradeep.shipments.domain.ShipmentStatus;
import com.pradeep.shipments.domain.TrackingEvent;
import com.pradeep.shipments.web.ShipmentDtos.CreateShipmentRequest;
import com.pradeep.shipments.web.ShipmentDtos.PageResponse;
import com.pradeep.shipments.web.ShipmentDtos.ShipmentResponse;
import com.pradeep.shipments.web.ShipmentDtos.ShipmentSummary;
import com.pradeep.shipments.web.ShipmentDtos.UpdateStatusRequest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShipmentRepository repository;
    private final Clock clock;

    public ShipmentService(ShipmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request) {
        LocalDate eta = DeliveryEstimator.estimate(LocalDate.now(clock), request.originPincode(),
                request.destinationPincode());
        Shipment shipment = new Shipment(newTrackingNumber(), request.senderName(), request.recipientName(),
                request.originPincode(), request.destinationPincode(), request.weightKg(), eta);
        shipment.addEvent(new TrackingEvent(shipment, ShipmentStatus.CREATED, request.originPincode(),
                "Shipment booked"));
        Shipment saved = repository.save(shipment);
        log.info("shipment created trackingNumber={} eta={}", saved.getTrackingNumber(), eta);
        return ShipmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse get(String trackingNumber) {
        return ShipmentResponse.from(load(trackingNumber));
    }

    @Transactional
    public ShipmentResponse updateStatus(String trackingNumber, UpdateStatusRequest request) {
        Shipment shipment = load(trackingNumber);
        ShipmentStatus previous = shipment.getStatus();
        shipment.transitionTo(request.status(), request.location(), request.note());
        log.info("shipment status changed trackingNumber={} from={} to={}", trackingNumber, previous,
                request.status());
        return ShipmentResponse.from(repository.saveAndFlush(shipment));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentSummary> list(ShipmentStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Shipment> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return new PageResponse<>(result.map(ShipmentSummary::from).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private Shipment load(String trackingNumber) {
        return repository.findByTrackingNumber(trackingNumber.toUpperCase())
                .orElseThrow(() -> new ShipmentNotFoundException(trackingNumber));
    }

    private String newTrackingNumber() {
        String candidate;
        do {
            candidate = "LQ" + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
        } while (repository.existsByTrackingNumber(candidate));
        return candidate;
    }
}
