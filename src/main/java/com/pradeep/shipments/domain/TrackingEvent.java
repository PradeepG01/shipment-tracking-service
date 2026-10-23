package com.pradeep.shipments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tracking_events")
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(nullable = false)
    private String location;

    @Column(length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected TrackingEvent() {
        // for JPA
    }

    public TrackingEvent(Shipment shipment, ShipmentStatus status, String location, String note) {
        this.shipment = shipment;
        this.status = status;
        this.location = location;
        this.note = note;
    }

    public Long getId() { return id; }
    public ShipmentStatus getStatus() { return status; }
    public String getLocation() { return location; }
    public String getNote() { return note; }
    public Instant getOccurredAt() { return occurredAt; }
}
