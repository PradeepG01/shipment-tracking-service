package com.pradeep.shipments.web;

import com.pradeep.shipments.domain.Shipment;
import com.pradeep.shipments.domain.ShipmentStatus;
import com.pradeep.shipments.domain.TrackingEvent;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs. Entities are never exposed directly, so the API contract stays stable. */
public final class ShipmentDtos {

    private ShipmentDtos() {
    }

    private static final String PINCODE = "^[1-9][0-9]{5}$";

    public record CreateShipmentRequest(
            @NotBlank @Size(max = 100) String senderName,
            @NotBlank @Size(max = 100) String recipientName,
            @NotBlank @Pattern(regexp = PINCODE, message = "must be a valid 6-digit Indian pincode") String originPincode,
            @NotBlank @Pattern(regexp = PINCODE, message = "must be a valid 6-digit Indian pincode") String destinationPincode,
            @NotNull @DecimalMin("0.01") @DecimalMax("1000.00") BigDecimal weightKg) {
    }

    public record UpdateStatusRequest(
            @NotNull ShipmentStatus status,
            @NotBlank @Size(max = 100) String location,
            @Size(max = 500) String note) {
    }

    public record TrackingEventResponse(ShipmentStatus status, String location, String note, Instant occurredAt) {
        static TrackingEventResponse from(TrackingEvent e) {
            return new TrackingEventResponse(e.getStatus(), e.getLocation(), e.getNote(), e.getOccurredAt());
        }
    }

    public record ShipmentResponse(
            String trackingNumber,
            String senderName,
            String recipientName,
            String originPincode,
            String destinationPincode,
            BigDecimal weightKg,
            ShipmentStatus status,
            LocalDate expectedDelivery,
            Instant createdAt,
            Instant updatedAt,
            List<TrackingEventResponse> events) {

        public static ShipmentResponse from(Shipment s) {
            return new ShipmentResponse(
                    s.getTrackingNumber(), s.getSenderName(), s.getRecipientName(), s.getOriginPincode(),
                    s.getDestinationPincode(), s.getWeightKg(), s.getStatus(), s.getExpectedDelivery(),
                    s.getCreatedAt(), s.getUpdatedAt(),
                    s.getEvents().stream().map(TrackingEventResponse::from).toList());
        }
    }

    public record ShipmentSummary(String trackingNumber, ShipmentStatus status, String destinationPincode,
                                  LocalDate expectedDelivery) {
        public static ShipmentSummary from(Shipment s) {
            return new ShipmentSummary(s.getTrackingNumber(), s.getStatus(), s.getDestinationPincode(),
                    s.getExpectedDelivery());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }
}
