package com.pradeep.shipments.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Shipment lifecycle as a finite state machine. Each status declares which statuses may follow it,
 * so an invalid update (e.g. DELIVERED -> IN_TRANSIT) is rejected in one place instead of
 * being checked ad hoc across the codebase.
 */
public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED,
    RETURNED,
    CANCELLED;

    public Set<ShipmentStatus> allowedNext() {
        return switch (this) {
            case CREATED -> EnumSet.of(PICKED_UP, CANCELLED);
            case PICKED_UP -> EnumSet.of(IN_TRANSIT, CANCELLED);
            case IN_TRANSIT -> EnumSet.of(IN_TRANSIT, OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> EnumSet.of(DELIVERED, DELIVERY_FAILED);
            case DELIVERY_FAILED -> EnumSet.of(OUT_FOR_DELIVERY, RETURNED);
            case DELIVERED, RETURNED, CANCELLED -> EnumSet.noneOf(ShipmentStatus.class);
        };
    }

    public boolean canTransitionTo(ShipmentStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
}
