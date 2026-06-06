import { get } from '../api';
import { ApiResponse } from '@/types/common';

export interface RevenueDetail {
  date: string;
  revenue: number;
}

export interface VenueRevenue {
  stadiumId: number;
  stadiumName: string;
  totalBookings: number;
  totalRevenue: number;
  occupancy: number;
  trend: string;
}

export interface RevenueReportResponse {
  totalRevenue: number;
  totalBookings: number;
  details: RevenueDetail[];
  venueRevenues: VenueRevenue[];
}

export const reportService = {
  getRevenueReport: (startDate: string, endDate: string, stadiumId?: number) => {
    return get<ApiResponse<RevenueReportResponse>>('/owner/reports/revenue', {
      params: { startDate, endDate, ...(stadiumId !== undefined && { stadiumId }) },
    });
  },
};
