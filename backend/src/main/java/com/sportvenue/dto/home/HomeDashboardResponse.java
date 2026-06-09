package com.sportvenue.dto.home;

import java.util.List;

public record HomeDashboardResponse(
        int totalBookingCount,
        int favoriteVenueCount,
        int rewardPoints,
        List<UpcomingBookingDto> upcomingBookings,
        List<VenueSummaryDto> favoriteVenues,
        List<VenueSummaryDto> recommendedVenues,
        List<CommunityEventDto> communityEvents,
        PersonalStatsDto personalStats
) {
}
