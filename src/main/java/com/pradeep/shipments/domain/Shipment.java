package com.pradeep.shipments.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 20)
    private String trackingNumber;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "origin_pincode", nullable = false, length = 6)
    private String originPincode;

    @Column(name = "destination_pincode", nullable = false, length = 6)
    private String destinationPincode;

    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "expected_delivery", nullable = false)
    private LocalDate expectedDelivery;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /** Optimistic locking: two concurrent status updates cannot silently overwrite each other. */
    @Version
    private long version;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC, id ASC")
    private List<TrackingEvent> events = new ArrayList<>();

    protected Shipment() {
        // for JPA
    }

    public Shipment(String trackingNumber, String senderName, String recipientName, String originPincode,
                    String destinationPincode, BigDecimal weightKg, LocalDate expectedDelivery) {
        this.trackingNumber = trackingNumber;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.originPincode = originPincode;
        this.destinationPincode = destinationPincode;
        this.weightKg = weightKg;
        this.expectedDelivery = expectedDelivery;
    }

    /** Applies a status change if the lifecycle allows it and records it as a tracking event. */
    public TrackingEvent transitionTo(ShipmentStatus next, String location, String note) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidTransitionException(status, next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
        TrackingEvent event = new TrackingEvent(this, next, location, note);
        events.add(event);
        return event;
    }

    public void addEvent(TrackingEvent event) {
        events.add(event);
    }

    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getSenderName() { return senderName; }
    public String getRecipientName() { return recipientName; }
    public String getOriginPincode() { return originPincode; }
    public String getDestinationPincode() { return destinationPincode; }
    public BigDecimal getWeightKg() { return weightKg; }
    public ShipmentStatus getStatus() { return status; }
    public LocalDate getExpectedDelivery() { return expectedDelivery; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<TrackingEvent> getEvents() { return events; }
}
