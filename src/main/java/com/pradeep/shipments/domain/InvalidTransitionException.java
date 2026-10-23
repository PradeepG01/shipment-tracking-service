package com.pradeep.shipments.domain;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(ShipmentStatus from, ShipmentStatus to) {
        super("Cannot move shipment from " + from + " to " + to + "; allowed: " + from.allowedNext());
    }
}
