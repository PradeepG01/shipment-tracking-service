package com.pradeep.shipments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pradeep.shipments.domain.InvalidTransitionException;
import com.pradeep.shipments.domain.Shipment;
import com.pradeep.shipments.domain.ShipmentStatus;
import com.pradeep.shipments.service.DeliveryEstimator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainTest {

    private Shipment newShipment() {
        return new Shipment("LQ0000000001", "A", "B", "560001", "110001", new BigDecimal("1.50"),
                LocalDate.of(2026, 11, 1));
    }

    @Test
    void followsTheHappyPathLifecycle() {
        Shipment s = newShipment();
        s.transitionTo(ShipmentStatus.PICKED_UP, "Bengaluru hub", null);
        s.transitionTo(ShipmentStatus.IN_TRANSIT, "Nagpur", null);
        s.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY, "Delhi", null);
        s.transitionTo(ShipmentStatus.DELIVERED, "Delhi", "Signed by recipient");
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(s.getStatus().isTerminal()).isTrue();
        assertThat(s.getEvents()).hasSize(4);
    }

    @Test
    void rejectsSkippingSteps() {
        Shipment s = newShipment();
        assertThatThrownBy(() -> s.transitionTo(ShipmentStatus.DELIVERED, "Delhi", null))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("CREATED");
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void allowsRetryAfterFailedDelivery() {
        assertThat(ShipmentStatus.DELIVERY_FAILED.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY)).isTrue();
        assertThat(ShipmentStatus.DELIVERED.allowedNext()).isEmpty();
    }

    @Test
    void estimatesTransitDaysFromPincodes() {
        assertThat(DeliveryEstimator.transitDays("560001", "560103")).isEqualTo(1);
        assertThat(DeliveryEstimator.transitDays("560001", "570001")).isEqualTo(3);
        assertThat(DeliveryEstimator.transitDays("560001", "110001")).isEqualTo(5);
    }

    @Test
    void skipsSundaysInDeliveryEstimate() {
        // Saturday 2026-10-24 + 1 working day -> Monday 2026-10-26
        assertThat(DeliveryEstimator.estimate(LocalDate.of(2026, 10, 24), "560001", "560002"))
                .isEqualTo(LocalDate.of(2026, 10, 26));
    }
}
