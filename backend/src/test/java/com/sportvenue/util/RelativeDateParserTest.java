package com.sportvenue.util;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class RelativeDateParserTest {

    // Fixed clock: 2026-07-17 (Friday)
    private static final Clock FRIDAY_CLOCK = Clock.fixed(
            Instant.parse("2026-07-17T00:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh")
    );

    @Test
    void testThu7TuanNay_FromFriday_ShouldBe_2026_07_18() {
        // Friday 2026-07-17, "thứ 7 tuần này" should be Saturday 2026-07-18
        RelativeDateParser parser = new RelativeDateParser(FRIDAY_CLOCK);

        LocalDate result = parser.parse("thứ 7 tuần này");

        System.out.println("=== Unit Test: thứ 7 tuần này ===");
        System.out.println("Current date (simulated): 2026-07-17 (Friday)");
        System.out.println("Input: 'thứ 7 tuần này'");
        System.out.println("Expected: 2026-07-18 (Saturday)");
        System.out.println("Actual: " + result);
        System.out.println("Result: " + (LocalDate.of(2026, 7, 18).equals(result) ? "PASS ✓" : "FAIL ✗"));

        assertEquals(LocalDate.of(2026, 7, 18), result,
            "thứ 7 tuần này from Friday 2026-07-17 should be 2026-07-18");
    }

    @Test
    void testThu7TuanSau_FromFriday_ShouldBe_2026_07_25() {
        // Friday 2026-07-17, "thứ 7 tuần sau" should be Saturday 2026-07-25
        RelativeDateParser parser = new RelativeDateParser(FRIDAY_CLOCK);

        LocalDate result = parser.parse("thứ 7 tuần sau");

        System.out.println("=== Unit Test: thứ 7 tuần sau ===");
        System.out.println("Current date (simulated): 2026-07-17 (Friday)");
        System.out.println("Input: 'thứ 7 tuần sau'");
        System.out.println("Expected: 2026-07-25 (Saturday)");
        System.out.println("Actual: " + result);
        System.out.println("Result: " + (LocalDate.of(2026, 7, 25).equals(result) ? "PASS ✓" : "FAIL ✗"));

        assertEquals(LocalDate.of(2026, 7, 25), result,
            "thứ 7 tuần sau from Friday 2026-07-17 should be 2026-07-25");
    }

    @Test
    void testNgayMai_FromFriday_ShouldBe_2026_07_18() {
        RelativeDateParser parser = new RelativeDateParser(FRIDAY_CLOCK);

        LocalDate result = parser.parse("ngày mai");

        System.out.println("=== Unit Test: ngày mai ===");
        System.out.println("Current date (simulated): 2026-07-17 (Friday)");
        System.out.println("Input: 'ngày mai'");
        System.out.println("Expected: 2026-07-18 (Saturday)");
        System.out.println("Actual: " + result);
        System.out.println("Result: " + (LocalDate.of(2026, 7, 18).equals(result) ? "PASS ✓" : "FAIL ✗"));

        assertEquals(LocalDate.of(2026, 7, 18), result);
    }

    @Test
    void testChuNhatTuanNay_FromFriday_ShouldBe_2026_07_19() {
        RelativeDateParser parser = new RelativeDateParser(FRIDAY_CLOCK);

        LocalDate result = parser.parse("chủ nhật tuần này");

        System.out.println("=== Unit Test: chủ nhật tuần này ===");
        System.out.println("Current date (simulated): 2026-07-17 (Friday)");
        System.out.println("Input: 'chủ nhật tuần này'");
        System.out.println("Expected: 2026-07-19 (Sunday)");
        System.out.println("Actual: " + result);
        System.out.println("Result: " + (LocalDate.of(2026, 7, 19).equals(result) ? "PASS ✓" : "FAIL ✗"));

        assertEquals(LocalDate.of(2026, 7, 19), result,
            "chủ nhật tuần này from Friday 2026-07-17 should be 2026-07-19");
    }

    @Test
    void testThu7TuanNay_FromThursday_ShouldBe_2026_07_18() {
        // Thursday 2026-07-16, "thứ 7 tuần này" should be Saturday 2026-07-18
        Clock thursdayClock = Clock.fixed(
                Instant.parse("2026-07-16T00:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh")
        );
        RelativeDateParser parser = new RelativeDateParser(thursdayClock);

        LocalDate result = parser.parse("thứ 7 tuần này");

        System.out.println("=== Unit Test: thứ 7 tuần này (from Thursday) ===");
        System.out.println("Current date (simulated): 2026-07-16 (Thursday)");
        System.out.println("Input: 'thứ 7 tuần này'");
        System.out.println("Expected: 2026-07-18 (Saturday)");
        System.out.println("Actual: " + result);
        System.out.println("Result: " + (LocalDate.of(2026, 7, 18).equals(result) ? "PASS ✓" : "FAIL ✗"));

        assertEquals(LocalDate.of(2026, 7, 18), result,
            "thứ 7 tuần này from Thursday 2026-07-16 should be 2026-07-18");
    }

    @Test
    void testThu7TuanNay_FromSaturday_ShouldBe_2026_07_25() {
        // Saturday 2026-07-18, "thứ 7 tuần này" should be NEXT Saturday 2026-07-25
        Clock saturdayClock = Clock.fixed(
                Instant.parse("2026-07-18T00:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh")
        );
        RelativeDateParser parser = new RelativeDateParser(saturdayClock);

        LocalDate result = parser.parse("thứ 7 tuần này");

        System.out.println("=== Unit Test: thứ 7 tuần này (from Saturday) ===");
        System.out.println("Current date (simulated): 2026-07-18 (Saturday)");
        System.out.println("Input: 'thứ 7 tuần này'");
        System.out.println("Expected: 2026-07-25 (Next Saturday)");
        System.out.println("Actual: " + result);
        System.out.println("Result: " + (LocalDate.of(2026, 7, 25).equals(result) ? "PASS ✓" : "FAIL ✗"));

        assertEquals(LocalDate.of(2026, 7, 25), result,
            "thứ 7 tuần này from Saturday 2026-07-18 should be 2026-07-25");
    }

    @Test
    void testThu4Standalone_FromFriday_ShouldBe_2026_07_22() {
        // Friday 2026-07-17, standalone "thứ 4" (no "tuần này/sau" qualifier) should be
        // Wednesday 2026-07-22, NOT Thursday. "thứ 4" = Wednesday (ISO day 3), so the parser
        // must subtract 1 from the raw number just like the qualified patterns do.
        RelativeDateParser parser = new RelativeDateParser(FRIDAY_CLOCK);

        LocalDate result = parser.parse("thứ 4");

        assertEquals(LocalDate.of(2026, 7, 22), result,
            "standalone 'thứ 4' from Friday 2026-07-17 should be Wednesday 2026-07-22");
    }
}
