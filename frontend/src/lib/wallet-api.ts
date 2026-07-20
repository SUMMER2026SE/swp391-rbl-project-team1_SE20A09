import { get, post } from "@/lib/api";
import type { BookingDetailResponse } from "@/lib/bookings-api";

export type WalletBalance = {
  balance: number;
};

export type WalletTransaction = {
  transactionId: number;
  amount: number;
  bookingId?: number | null;
  note?: string;
  transactionType: string;
  createdAt: string;
};

type PageResponse<T> = {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type WalletTransactionPageResult = {
  transactions: WalletTransaction[];
  totalElements: number;
  totalPages: number;
  pageNo: number;
  last: boolean;
};

// --- Owner API ---

export async function fetchOwnerWalletBalance(): Promise<WalletBalance> {
  return get<WalletBalance>("/owner/wallet");
}

export async function fetchOwnerWalletTransactions(
  page = 0,
  size = 10
): Promise<WalletTransactionPageResult> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const data = await get<PageResponse<WalletTransaction>>(`/owner/wallet/transactions?${query.toString()}`);
  return {
    transactions: data.content,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    pageNo: data.pageNumber,
    last: data.last,
  };
}

export async function fetchWalletTransactionsByBooking(
  bookingId: number
): Promise<WalletTransaction[]> {
  return get<WalletTransaction[]>(`/owner/wallet/transactions/booking/${bookingId}`);
}

// --- Customer API ---

export async function fetchCustomerWalletBalance(): Promise<WalletBalance> {
  return get<WalletBalance>("/wallet");
}

export async function fetchCustomerWalletTransactions(
  page = 0,
  size = 10
): Promise<WalletTransactionPageResult> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const data = await get<PageResponse<WalletTransaction>>(`/wallet/transactions?${query.toString()}`);
  return {
    transactions: data.content,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    pageNo: data.pageNumber,
    last: data.last,
  };
}

export type WalletTopupResponse = {
  paymentUrl: string;
};

export async function initiateWalletTopup(amount: number): Promise<WalletTopupResponse> {
  return post<WalletTopupResponse>("/wallet/topup", { amount });
}

export async function payBookingWithWallet(
  bookingId: number,
  paymentOption: "FULL" | "DEPOSIT" = "FULL"
): Promise<BookingDetailResponse> {
  return post<BookingDetailResponse>(
    `/bookings/${bookingId}/pay-with-wallet?paymentOption=${paymentOption}`
  );
}

export async function payRemainingWithWallet(bookingId: number): Promise<BookingDetailResponse> {
  return post<BookingDetailResponse>(`/bookings/${bookingId}/pay-remaining-with-wallet`);
}

// --- Admin API ---

export async function fetchAdminWalletBalance(): Promise<WalletBalance> {
  return get<WalletBalance>("/admin/wallet");
}

export async function fetchAdminWalletTransactions(
  page = 0,
  size = 10
): Promise<WalletTransactionPageResult> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const data = await get<PageResponse<WalletTransaction>>(`/admin/wallet/transactions?${query.toString()}`);
  return {
    transactions: data.content,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    pageNo: data.pageNumber,
    last: data.last,
  };
}
