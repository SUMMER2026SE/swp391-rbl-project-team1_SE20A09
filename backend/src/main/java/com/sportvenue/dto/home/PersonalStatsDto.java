package com.sportvenue.dto.home;

public record PersonalStatsDto(
        int totalHours,
        long venuesVisited,
        String favoriteSport
) {
}
