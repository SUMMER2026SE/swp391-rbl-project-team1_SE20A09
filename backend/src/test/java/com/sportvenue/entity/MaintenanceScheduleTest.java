package com.sportvenue.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test primitive {@code overlaps()} — cốt lõi của việc mở rộng MaintenanceSchedule sang khung giờ.
 * Không phụ thuộc đồng hồ hệ thống, mọi input đều tường minh để tránh flaky.
 */
class MaintenanceScheduleTest {

    private static final LocalDate DAY = LocalDate.of(2026, 7, 10);

    @Test
    void wholeDaySchedule_noTimeSet_coversEntireDay() {
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .startDate(DAY).endDate(DAY)
                .build();

        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(0, 0)),
                LocalDateTime.of(DAY, LocalTime.of(0, 1))))
                .isTrue();
        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(23, 0)),
                LocalDateTime.of(DAY, LocalTime.of(23, 59))))
                .isTrue();
    }

    @Test
    void hourScopedSchedule_slotBeforeWindow_doesNotOverlap() {
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .startDate(DAY).endDate(DAY)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();

        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(9, 0)),
                LocalDateTime.of(DAY, LocalTime.of(10, 0))))
                .isFalse();
    }

    @Test
    void hourScopedSchedule_slotAfterWindow_doesNotOverlap() {
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .startDate(DAY).endDate(DAY)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();

        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(17, 0)),
                LocalDateTime.of(DAY, LocalTime.of(18, 0))))
                .isFalse();
    }

    @Test
    void hourScopedSchedule_slotPartiallyOverlapsWindow_returnsTrue() {
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .startDate(DAY).endDate(DAY)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();

        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(15, 30)),
                LocalDateTime.of(DAY, LocalTime.of(17, 0))))
                .isTrue();
    }

    @Test
    void crossMidnightSchedule_overlapsBothDays() {
        // Bảo trì xuyên đêm: hôm nay 22:00 -> ngày mai 02:00.
        LocalDate tomorrow = DAY.plusDays(1);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .startDate(DAY).endDate(tomorrow)
                .startTime(LocalTime.of(22, 0)).endTime(LocalTime.of(2, 0))
                .build();

        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(23, 0)),
                LocalDateTime.of(tomorrow, LocalTime.of(0, 0))))
                .isTrue();
        assertThat(schedule.overlaps(
                LocalDateTime.of(tomorrow, LocalTime.of(1, 0)),
                LocalDateTime.of(tomorrow, LocalTime.of(3, 0))))
                .isTrue();
        // Trưa hôm nay — trước 22:00 -> không đụng.
        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(12, 0)),
                LocalDateTime.of(DAY, LocalTime.of(13, 0))))
                .isFalse();
    }

    @Test
    void indefiniteSchedule_noEndDate_neverEnds() {
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .startDate(DAY).endDate(null)
                .startTime(LocalTime.of(10, 0))
                .build();

        assertThat(schedule.overlaps(
                LocalDateTime.of(DAY.plusYears(50), LocalTime.of(10, 30)),
                LocalDateTime.of(DAY.plusYears(50), LocalTime.of(11, 0))))
                .isTrue();
    }

    @Test
    void twoNonOverlappingWindowsSameDay_doNotOverlapEachOther() {
        MaintenanceSchedule morning = MaintenanceSchedule.builder()
                .startDate(DAY).endDate(DAY)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .build();

        assertThat(morning.overlaps(
                LocalDateTime.of(DAY, LocalTime.of(20, 0)),
                LocalDateTime.of(DAY, LocalTime.of(22, 0))))
                .isFalse();
    }
}
