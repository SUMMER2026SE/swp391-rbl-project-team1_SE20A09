package com.sportvenue.util;

import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StadiumUtilsTest {

    @Test
    void resolveAddress_usesOwnAddressWhenPresent() {
        Stadium stadium = Stadium.builder()
                .address("123 Đường A")
                .complex(StadiumComplex.builder().address("456 Đường B").build())
                .build();

        assertEquals("123 Đường A", StadiumUtils.resolveAddress(stadium));
    }

    @Test
    void resolveAddress_fallsBackToComplexForChildNode() {
        Stadium stadium = Stadium.builder()
                .complex(StadiumComplex.builder().address("456 Đường B").build())
                .build();

        assertEquals("456 Đường B", StadiumUtils.resolveAddress(stadium));
    }
}
