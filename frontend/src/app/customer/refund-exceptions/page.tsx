"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Clock, Calendar, HelpCircle, ChevronRight, AlertCircle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { get, put } from "@/lib/api";
import { toast } from "sonner";

interface RefundException {
  requestId: number;
  bookingId: number;
  stadiumName: string;
  reservationDate: string;
  reason: string;
  evidenceUrl: string;
  status: string;
  ownerNote: string;
  adminNote: string;
  refundPercent: number | null;
  createdAt: string;
  expiresAt: string;
  canEscalate: boolean;
  isExpired: boolean;
}

interface PageResponse {
  content: RefundException[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

const EXCEPTION_STATUS_CONFIG = {
  PENDING_OWNER: { label: "Chờ Owner duyệt", className: "bg-amber-50 text-amber-700 border-amber-200" },
  APPROVED_OWNER: { label: "Chấp nhận (Chờ hoàn)", className: "bg-green-50 text-green-700 border-green-200" },
  REJECTED_OWNER: { label: "Owner từ chối", className: "bg-red-50 text-red-700 border-red-200" },
  PENDING_ADMIN: { label: "Chờ Admin duyệt (Leo thang)", className: "bg-indigo-50 text-indigo-700 border-indigo-200" },
  APPROVED_ADMIN: { label: "Đã hoàn tiền (Admin)", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
  REJECTED_ADMIN: { label: "Từ chối cuối (Admin)", className: "bg-slate-50 text-slate-700 border-slate-200" },
  EXPIRED: { label: "Đã hết hạn", className: "bg-slate-50 text-slate-500 border-slate-200" },
} as const;

export default function CustomerRefundExceptionsPage() {
  const [data, setData] = useState<PageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [escalatingId, setEscalatingId] = useState<number | null>(null);

  const loadRequests = async (page = 0) => {
    try {
      setLoading(true);
      const res = await get<PageResponse>(`/refund-exceptions/me?page=${page}&size=10`);
      setData(res);
    } catch (err: any) {
      setError(err.message || "Không thể tải danh sách yêu cầu ngoại lệ.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, []);

  const handleEscalate = async (requestId: number) => {
    try {
      setEscalatingId(requestId);
      const updated = await put<RefundException>(`/refund-exceptions/${requestId}/escalate`);
      toast.success("Đã leo thang yêu cầu lên Ban quản trị thành công!");
      
      // Update local state
      if (data) {
        setData({
          ...data,
          content: data.content.map(item => item.requestId === requestId ? updated : item)
        });
      }
    } catch (err: any) {
      toast.error(err.message || "Không thể leo thang yêu cầu.");
    } finally {
      setEscalatingId(null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Header />

      <main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
        <div className="mb-6 flex items-center gap-3">
          <Link href="/profile?tab=bookings" className="text-slate-500 hover:text-primary transition-colors">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Yêu cầu ngoại lệ hoàn tiền</h1>
            <p className="text-sm text-slate-500 font-medium">Xem danh sách các đơn xin hoàn tiền do lý do bất khả kháng</p>
          </div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="h-10 w-10 animate-spin text-primary" />
          </div>
        ) : error ? (
          <Card className="rounded-3xl border-none shadow-sm p-6 text-center space-y-4">
            <AlertCircle className="h-12 w-12 text-red-500 mx-auto" />
            <h3 className="font-bold text-slate-900 text-lg">Đã xảy ra lỗi</h3>
            <p className="text-slate-500 max-w-sm mx-auto">{error}</p>
            <Button onClick={() => loadRequests()} variant="outline" className="rounded-xl">Thử lại</Button>
          </Card>
        ) : !data || data.content.length === 0 ? (
          <Card className="rounded-3xl border-none shadow-sm p-12 text-center space-y-4">
            <HelpCircle className="h-16 w-16 text-slate-300 mx-auto" />
            <h3 className="font-bold text-slate-700 text-lg">Không có yêu cầu nào</h3>
            <p className="text-slate-500 max-w-xs mx-auto">
              Bạn chưa gửi yêu cầu xin xem xét ngoại lệ hoàn tiền nào.
            </p>
            <Button asChild className="rounded-xl bg-slate-900 hover:bg-slate-800 text-white">
              <Link href="/profile?tab=bookings">Xem lịch sử đặt sân</Link>
            </Button>
          </Card>
        ) : (
          <div className="space-y-4">
            {data.content.map((req) => (
              <Card key={req.requestId} className="rounded-3xl border-none shadow-sm bg-white overflow-hidden hover:shadow-md transition-shadow">
                <CardContent className="p-6 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                    <div className="space-y-1">
                      <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Đơn đặt sân #{req.bookingId}</span>
                      <h3 className="font-bold text-slate-800 text-base flex items-center gap-1.5">
                        {req.stadiumName}
                      </h3>
                    </div>
                    <Badge variant="outline" className={`${EXCEPTION_STATUS_CONFIG[req.status as keyof typeof EXCEPTION_STATUS_CONFIG]?.className || "bg-slate-50 text-slate-600"} font-bold px-3 py-1 rounded-full text-[10px] uppercase tracking-wider`}>
                      {EXCEPTION_STATUS_CONFIG[req.status as keyof typeof EXCEPTION_STATUS_CONFIG]?.label || req.status}
                    </Badge>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs font-medium text-slate-500 py-2 border-y border-slate-100">
                    <div className="flex items-center gap-2">
                      <Calendar className="h-4 w-4 text-slate-400" />
                      <span>Ngày chơi: {req.reservationDate}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock className="h-4 w-4 text-slate-400" />
                      <span>Ngày yêu cầu: {new Date(req.createdAt).toLocaleDateString("vi-VN")}</span>
                    </div>
                  </div>

                  <div className="space-y-2 text-sm">
                    <p className="text-slate-600 leading-relaxed italic">
                      <span className="font-semibold text-slate-800 not-italic block mb-0.5">Lý do bất khả kháng:</span>
                      "{req.reason}"
                    </p>

                    {req.evidenceUrl && (
                      <div className="space-y-1.5">
                        <span className="text-xs font-semibold text-slate-700">Bằng chứng đã gửi:</span>
                        <a
                          href={req.evidenceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="block border border-slate-200 rounded-xl overflow-hidden bg-slate-50 hover:opacity-90 transition-opacity max-w-xs"
                        >
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img
                            src={req.evidenceUrl}
                            alt="Bằng chứng bất khả kháng"
                            className="w-full object-contain max-h-52 bg-white"
                            onError={(e) => {
                              (e.currentTarget as HTMLImageElement).style.display = "none";
                              e.currentTarget.nextElementSibling?.classList.remove("hidden");
                            }}
                          />
                          <span className="hidden block px-3 py-2 text-xs text-primary break-all">
                            {req.evidenceUrl}
                          </span>
                        </a>
                      </div>
                    )}

                    {req.refundPercent !== null && (
                      <div className="text-xs font-semibold text-slate-800">
                        Mức hoàn tiền được duyệt: <span className="text-emerald-600 text-sm font-bold">{req.refundPercent}%</span>
                      </div>
                    )}

                    {req.ownerNote && (
                      <p className="text-slate-600 leading-relaxed bg-slate-50 p-3 rounded-2xl">
                        <span className="font-semibold text-slate-800 block mb-0.5">Chủ sân phản hồi:</span>
                        "{req.ownerNote}"
                      </p>
                    )}

                    {req.adminNote && (
                      <p className="text-slate-600 leading-relaxed bg-slate-50 p-3 rounded-2xl">
                        <span className="font-semibold text-slate-800 block mb-0.5">Ban quản trị quyết định:</span>
                        "{req.adminNote}"
                      </p>
                    )}
                  </div>

                  <div className="flex flex-col sm:flex-row justify-end gap-3 pt-2">
                    {req.canEscalate && (
                      <Button
                        onClick={() => handleEscalate(req.requestId)}
                        disabled={escalatingId !== null}
                        className="rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold h-10 px-6 text-xs"
                      >
                        {escalatingId === req.requestId ? (
                          <>
                            <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" />
                            Đang xử lý...
                          </>
                        ) : (
                          "Leo thang lên Admin"
                        )}
                      </Button>
                    )}
                    <Button asChild variant="outline" className="rounded-xl border-slate-200 text-xs font-semibold h-10">
                      <Link href={`/booking/${req.bookingId}`}>
                        Chi tiết đơn đặt
                        <ChevronRight className="ml-1.5 h-3.5 w-3.5" />
                      </Link>
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}

            {data.totalPages > 1 && (
              <div className="flex justify-center gap-2 pt-4">
                {Array.from({ length: data.totalPages }).map((_, idx) => (
                  <Button
                    key={idx}
                    variant={data.pageNumber === idx ? "default" : "outline"}
                    className="h-8 w-8 p-0 rounded-lg text-xs"
                    onClick={() => loadRequests(idx)}
                  >
                    {idx + 1}
                  </Button>
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}
