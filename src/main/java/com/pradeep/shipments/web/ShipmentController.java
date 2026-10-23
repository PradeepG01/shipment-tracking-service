package com.pradeep.shipments.web;

import com.pradeep.shipments.domain.ShipmentStatus;
import com.pradeep.shipments.service.ShipmentService;
import com.pradeep.shipments.web.ShipmentDtos.CreateShipmentRequest;
import com.pradeep.shipments.web.ShipmentDtos.PageResponse;
import com.pradeep.shipments.web.ShipmentDtos.ShipmentResponse;
import com.pradeep.shipments.web.ShipmentDtos.ShipmentSummary;
import com.pradeep.shipments.web.ShipmentDtos.UpdateStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipments")
@Validated
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/shipments/" + created.trackingNumber())).body(created);
    }

    @GetMapping("/{trackingNumber}")
    public ShipmentResponse get(@PathVariable String trackingNumber) {
        return service.get(trackingNumber);
    }

    @PatchMapping("/{trackingNumber}/status")
    public ShipmentResponse updateStatus(@PathVariable String trackingNumber,
                                         @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(trackingNumber, request);
    }

    @GetMapping
    public PageResponse<ShipmentSummary> list(@RequestParam(required = false) ShipmentStatus status,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(status, page, size);
    }
}
