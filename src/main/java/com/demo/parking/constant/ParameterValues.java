package com.demo.parking.constant;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ParameterValues {
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    public static final String RESERVATION_ID_PREFIX = "RES-";
    public static final String INVOICE_ID_PREFIX = "INV-";

    public static final String LOT_ID_REGEX = "LOT-[A-Z0-9]{3,8}";
    public static final String SLOT_ID_REGEX = "[A-Z]{1,3}-\\d{1,4}";
    public static final String VEHICLE_TYPE_REGEX = VehicleType.MOTORCYCLE + "|" + VehicleType.CAR + "|" + VehicleType.TRUCK;

    public static final long SECONDS_PER_MINUTE = 60;
    public static final long BLOCK_SECONDS = Lengths.BILLING_BLOCK_MINUTES * SECONDS_PER_MINUTE;
    public static final BigDecimal BLOCKS_PER_HOUR = BigDecimal.valueOf(60L / Lengths.BILLING_BLOCK_MINUTES);
}
