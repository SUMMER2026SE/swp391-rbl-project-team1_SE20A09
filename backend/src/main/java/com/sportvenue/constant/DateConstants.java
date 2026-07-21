package com.sportvenue.constant;

import java.time.LocalDate;

public final class DateConstants {

    private DateConstants() {
        // Utility class
    }

    public static final LocalDate EPOCH_START = LocalDate.of(2000, 1, 1);
    public static final LocalDate EPOCH_END = LocalDate.of(2100, 1, 1);
}
