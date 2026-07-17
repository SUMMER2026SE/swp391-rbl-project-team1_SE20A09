"use client";

import { useState, useEffect, useCallback } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  WalletCards,
  ArrowUpRight,
  ArrowDownLeft,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Calendar,
  DollarSign,
  AlertCircle
} from "lucide-react";
import { toast } from "sonner";
import { fetchOwnerWalletBalance, fetchOwnerWalletTransactions, WalletTransaction } from "@/lib/wallet-api";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import Link from "next/link";

export default function OwnerWalletPage() {
  const [balance, setBalance] = useState<number | null>(null);
  const [transactions, setTransactions] = useState<WalletTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const size = 10;

  const loadWalletData = useCallback(async () => {
    setLoading(true);
    try {
      const [balRes, txRes] = await Promise.all([
        fetchOwnerWalletBalance(),
        fetchOwnerWalletTransactions(page, size)
      ]);
      setBalance(balRes.balance);
      setTransactions(txRes.transactions);
      setTotalPages(txRes.totalPages);
      setTotalElements(txRes.totalElements);
    } catch (err: any) {
      console.error(err);
      toast.error("Không thể tải thông tin ví và giao dịch.");
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadWalletData();
  }, [loadWalletData]);

  const getTransactionBadge = (type: string) => {
    switch (type) {
      case "BOOKING_CREDIT":
        return <Badge className="bg-emerald-50 text-emerald-700 hover:bg-emerald-50 border-emerald-200">Cộng doanh thu</Badge>;
      case "SERVICE_FEE_DEBIT":
        return <Badge className="bg-amber-50 text-amber-700 hover:bg-amber-50 border-amber-200">Khấu trừ phí mặt</Badge>;
      case "REFUND_DEBIT":
        return <Badge className="bg-rose-50 text-rose-700 hover:bg-rose-50 border-rose-200">Trừ hoàn tiền</Badge>;
      default:
        return <Badge variant="outline">{type}</Badge>;
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto p-4 md:p-6">
      {/* Top Section: Balance Card */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="md:col-span-2 bg-gradient-to-br from-emerald-600 to-teal-700 text-white shadow-xl border-none relative overflow-hidden group">
          <div className="absolute -right-10 -top-10 bg-white/10 w-40 h-40 rounded-full blur-2xl group-hover:bg-white/20 transition-all"></div>
          <CardContent className="p-6 md:p-8 flex flex-col justify-between h-full min-h-[180px]">
            <div>
              <div className="flex items-center gap-2 text-emerald-100/90 text-sm font-semibold uppercase tracking-wider mb-2">
                <WalletCards className="h-5 w-5" />
                Ví tài khoản chủ sân
              </div>
              <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mt-2">
                {balance !== null ? `${balance.toLocaleString("vi-VN")} đ` : "---"}
              </h1>
            </div>
            <div className="mt-6 flex flex-wrap gap-3">
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <span>
                      <Button
                        disabled
                        className="bg-white text-emerald-700 hover:bg-slate-100 font-semibold px-6 py-2.5 rounded-xl disabled:opacity-70 disabled:cursor-not-allowed transition-all"
                      >
                        Yêu cầu rút tiền
                      </Button>
                    </span>
                  </TooltipTrigger>
                  <TooltipContent className="bg-slate-800 text-white border-none p-3 rounded-lg text-xs max-w-xs shadow-lg">
                    Tính năng rút tiền thật sẽ khả dụng sau khi triển khai production với đối tác ngân hàng.
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            </div>
          </CardContent>
        </Card>

        {/* Small Analytics info */}
        <Card className="border-slate-200 shadow-sm bg-white dark:bg-slate-900">
          <CardHeader className="pb-2">
            <CardTitle className="text-slate-600 text-xs font-semibold uppercase tracking-wider">Thông tin ví</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center py-1.5 border-b border-slate-100">
              <span className="text-slate-500 text-sm">Loại ví</span>
              <span className="font-semibold text-sm text-slate-800">Ví chủ sân</span>
            </div>
            <div className="flex justify-between items-center py-1.5 border-b border-slate-100">
              <span className="text-slate-500 text-sm">Tổng giao dịch</span>
              <span className="font-semibold text-sm text-slate-800">{totalElements}</span>
            </div>
            <div className="flex justify-between items-center py-1.5">
              <span className="text-slate-500 text-sm">Đơn vị tiền tệ</span>
              <span className="font-semibold text-sm text-slate-800">VND (đ)</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Transactions Section */}
      <Card className="border border-slate-200 shadow-sm bg-white">
        <CardHeader className="flex flex-row items-center justify-between border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-2">
            Lịch sử giao dịch ví (Ledger)
          </CardTitle>
          <span className="text-xs text-muted-foreground bg-slate-100 px-2.5 py-1 rounded-full font-medium">
            Tổng cộng: {totalElements}
          </span>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3 text-slate-500">
              <Loader2 className="h-8 w-8 animate-spin text-emerald-600" />
              <span className="text-sm">Đang tải lịch sử giao dịch...</span>
            </div>
          ) : transactions.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-slate-400 gap-2">
              <AlertCircle className="h-10 w-10 text-slate-300" />
              <span className="text-sm font-medium">Chưa có giao dịch nào được thực hiện.</span>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader className="bg-slate-50/50">
                  <TableRow>
                    <TableHead className="w-[120px] font-bold text-slate-700">Mã GD</TableHead>
                    <TableHead className="font-bold text-slate-700">Mã Booking</TableHead>
                    <TableHead className="font-bold text-slate-700">Loại giao dịch</TableHead>
                    <TableHead className="font-bold text-slate-700">Mô tả / Note</TableHead>
                    <TableHead className="font-bold text-slate-700">Ngày tạo</TableHead>
                    <TableHead className="text-right font-bold text-slate-700 pr-6">Số tiền</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {transactions.map((tx) => {
                    const isPositive = tx.amount > 0;
                    return (
                      <TableRow key={tx.transactionId} className="hover:bg-slate-50/50 transition-colors">
                        <TableCell className="font-medium text-slate-600">
                          #{tx.transactionId}
                        </TableCell>
                        <TableCell>
                          {tx.bookingId ? (
                            <Link
                              href={`/owner/bookings`}
                              className="text-emerald-600 hover:text-emerald-700 font-semibold hover:underline"
                            >
                              BK{String(tx.bookingId).padStart(6, "0")}
                            </Link>
                          ) : (
                            <span className="text-slate-400">N/A</span>
                          )}
                        </TableCell>
                        <TableCell>
                          {getTransactionBadge(tx.transactionType)}
                        </TableCell>
                        <TableCell className="max-w-[280px] truncate text-slate-600 text-sm">
                          {tx.note || "---"}
                        </TableCell>
                        <TableCell className="text-slate-500 text-xs">
                          <div className="flex items-center gap-1.5">
                            <Calendar className="h-3.5 w-3.5" />
                            {format(new Date(tx.createdAt), "dd/MM/yyyy HH:mm", { locale: vi })}
                          </div>
                        </TableCell>
                        <TableCell className={`text-right font-bold pr-6 text-sm ${
                          isPositive ? "text-emerald-600" : "text-rose-600"
                        }`}>
                          <div className="flex items-center justify-end gap-1">
                            {isPositive ? (
                              <ArrowUpRight className="h-4 w-4 text-emerald-500" />
                            ) : (
                              <ArrowDownLeft className="h-4 w-4 text-rose-500" />
                            )}
                            {isPositive ? "+" : ""}
                            {tx.amount.toLocaleString("vi-VN")} đ
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>

        {/* Pagination Section */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-100 px-6 py-4">
            <span className="text-xs text-slate-500 font-medium">
              Trang {page + 1} / {totalPages}
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="h-8 w-8 p-0"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page === totalPages - 1}
                className="h-8 w-8 p-0"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
