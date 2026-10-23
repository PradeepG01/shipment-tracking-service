package com.pradeep.shipments.service;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(String trackingNumber) {
        super("No shipment with tracking number " + trackingNumber);
    }
}
