import { get, post } from "@/lib/api";
import type { PageResponse } from "@/types/common";

export type ReportCategory =
  | "NO_SHOW"
  | "PROPERTY_DAMAGE"
  | "HARASSMENT"
  | "FRAUD"
  | "PAYMENT_ABUSE"
  | "FAKE_LISTING"
  | "OTHER";

export type ReportStatus = "OPEN" | "UNDER_REVIEW" | "ACTION_TAKEN" | "DISMISSED";

export type CreateUserReportRequest = {
  reporteeId: number;
  bookingId?: number;
  matchRequestId?: number;
  joinRequestId?: number;
  stadiumId?: number;
  category: ReportCategory;
  description: string;
  evidenceUrls?: string[];
};

export type UserReportResponse = {
  reportId: number;
  category: ReportCategory;
  status: string;
  createdAt?: string;
};

type ReportUserSummary = {
  userId: number;
  fullName: string;
  email: string;
  roleName: string;
};

export type MyReport = {
  reportId: number;
  reportee: ReportUserSummary;
  category: ReportCategory;
  description: string;
  evidenceUrls: string[];
  status: ReportStatus;
  resolutionNote?: string;
  resolvedAt?: string;
  createdAt: string;
};

export async function createUserReport(payload: CreateUserReportRequest): Promise<UserReportResponse> {
  return post<UserReportResponse>("/reports", payload);
}

export async function getMyReports(page = 0, size = 20): Promise<PageResponse<MyReport>> {
  return get<PageResponse<MyReport>>("/reports/me", { params: { page, size } });
}
