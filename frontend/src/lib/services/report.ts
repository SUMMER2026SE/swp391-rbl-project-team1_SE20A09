import { get } from '../api';

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

export interface ApiResponse<T> {
  code: number;
  message?: string;
  result: T;
}

export const reportService = {
  getRevenueReport: (startDate: string, endDate: string, stadiumId?: number) => {
    return get<ApiResponse<RevenueReportResponse>>('/owner/reports/revenue', {
      params: { startDate, endDate, stadiumId },
    });
  },
};
