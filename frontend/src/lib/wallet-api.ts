import { get } from "@/lib/api";

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
