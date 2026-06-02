import { get } from '../api';

export interface RevenueDetail {
  date: string;
  revenue: number;
}

export interface RevenueReportResponse {
  totalRevenue: number;
  totalBookings: number;
  details: RevenueDetail[];
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
