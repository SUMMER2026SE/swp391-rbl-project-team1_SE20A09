import { post } from "@/lib/api";

export type ReportCategory =
  | "NO_SHOW"
  | "PROPERTY_DAMAGE"
  | "HARASSMENT"
  | "FRAUD"
  | "PAYMENT_ABUSE"
  | "FAKE_LISTING"
  | "OTHER";

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

export async function createUserReport(payload: CreateUserReportRequest): Promise<UserReportResponse> {
  return post<UserReportResponse>("/reports", payload);
}
