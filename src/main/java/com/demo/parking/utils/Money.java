package com.demo.parking.utils;

import com.demo.parking.constant.Lengths;
import com.demo.parking.constant.ParameterValues;

import java.math.BigDecimal;

/**
 * Money helpers. All amounts are BigDecimal, rounded HALF_UP to 2 decimals.
 */
public final class Money {

    public static final BigDecimal ZERO = scale(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal scale(BigDecimal amount) {
        return amount.setScale(Lengths.MONEY_SCALE, ParameterValues.MONEY_ROUNDING);
    }

    public static BigDecimal orZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
