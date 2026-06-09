package com.sportvenue.dto.home;

public record UpcomingBookingDto(
        String id,
        String venueName,
        String sportType,
        String location,
        String date,
        String time,
        String status,
        String imageUrl
) {
}
