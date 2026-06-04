package com.sportvenue.dto.home;

public record CommunityEventDto(
        String id,
        String title,
        String sportType,
        String datetime,
        String location,
        int slotsNeeded
) {
}
