package com.pradeep.shipments.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Estimates delivery dates from Indian pincodes. The first digit of a pincode is the postal region and
 * the first three digits are the sorting district, so shared prefixes approximate distance.
 * Sundays are skipped because they are non-delivery days.
 */
public final class DeliveryEstimator {

    private DeliveryEstimator() {
    }

    public static int transitDays(String originPincode, String destinationPincode) {
        if (originPincode.substring(0, 3).equals(destinationPincode.substring(0, 3))) {
            return 1; // same sorting district
        }
        if (originPincode.charAt(0) == destinationPincode.charAt(0)) {
            return 3; // same postal region
        }
        return 5; // inter-region
    }

    public static LocalDate estimate(LocalDate shipDate, String originPincode, String destinationPincode) {
        int remaining = transitDays(originPincode, destinationPincode);
        LocalDate date = shipDate;
        while (remaining > 0) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                remaining--;
            }
        }
        return date;
    }
}
