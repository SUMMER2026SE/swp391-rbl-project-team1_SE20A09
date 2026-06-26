package com.sportvenue.dto.home;

import java.util.List;

public record HomeDashboardResponse(
        int totalBookingCount,
        int recentlyPlayedVenueCount,
        int rewardPoints,
        List<UpcomingBookingDto> upcomingBookings,
        List<VenueSummaryDto> recentlyPlayedVenues,
        List<VenueSummaryDto> recommendedVenues,
        List<CommunityEventDto> communityEvents,
        PersonalStatsDto personalStats
) {
}
