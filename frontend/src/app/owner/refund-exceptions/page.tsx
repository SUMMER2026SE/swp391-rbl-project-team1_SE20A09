"use client";

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select";
import { Loader2, AlertCircle, FileText, Check, X, Calendar, User, Info, Clock } from "lucide-react";
import { get, put } from "@/lib/api";
import { toast } from "sonner";

interface RefundException {
  requestId: number;
  bookingId: number;
  stadiumName: string;
  reservationDate: string;
  customerName: string;
  reason: string;
  evidenceUrl: string;
  status: string;
  ownerNote: string;
  adminNote: string;
  refundPercent: number | null;
  createdAt: string;
  expiresAt: string;
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
  PENDING_OWNER: { label: "Chờ duyệt", className: "bg-amber-50 text-amber-700 border-amber-200" },
  APPROVED_OWNER: { label: "Chấp nhận (Chờ hoàn)", className: "bg-green-50 text-green-700 border-green-200" },
  REJECTED_OWNER: { label: "Đã từ chối", className: "bg-red-50 text-red-700 border-red-200" },
  PENDING_ADMIN: { label: "Chờ Admin duyệt", className: "bg-indigo-50 text-indigo-700 border-indigo-200" },
  APPROVED_ADMIN: { label: "Đã hoàn tiền (Admin)", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
  REJECTED_ADMIN: { label: "Từ chối cuối (Admin)", className: "bg-slate-50 text-slate-700 border-slate-200" },
  EXPIRED: { label: "Đã hết hạn", className: "bg-slate-50 text-slate-500 border-slate-200" },
} as const;

export default function OwnerRefundExceptionsPage() {
  const [data, setData] = useState<PageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  // Review Dialog state
  const [selectedRequest, setSelectedRequest] = useState<RefundException | null>(null);
  const [approved, setApproved] = useState<boolean | null>(null);
  const [refundPercent, setRefundPercent] = useState<string>("50"); // approved=true must be 50 or 100
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const fetchRequests = async (pageNumber = 0) => {
    try {
      setLoading(true);
      const res = await get<PageResponse>(`/owner/refund-exceptions?page=${pageNumber}&size=10`);
      setData(res);
      setPage(pageNumber);
    } catch (err: any) {
      setError(err.message || "Không thể tải danh sách yêu cầu.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const openReviewModal = (req: RefundException, approveStatus: boolean) => {
    setSelectedRequest(req);
    setApproved(approveStatus);
    setRefundPercent("50");
    setNote("");
  };

  const handleReviewSubmit = async () => {
    if (!selectedRequest || approved === null) return;

    setSubmitting(true);
    try {
      const payload = {
        approved,
        refundPercent: approved ? parseInt(refundPercent, 10) : null,
        ownerNote: note.trim() || null,
      };

      await put(`/owner/refund-exceptions/${selectedRequest.requestId}/review`, payload);
      toast.success(approved ? "Đã duyệt chấp thuận yêu cầu hoàn tiền." : "Đã từ chối yêu cầu.");
      setSelectedRequest(null);
      fetchRequests(page);
    } catch (err: any) {
      toast.error(err.message || "Có lỗi xảy ra khi cập nhật yêu cầu.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-10 w-10 animate-spin text-primary" />
        </div>
      ) : error ? (
        <Card className="rounded-3xl border-none shadow-sm p-6 text-center space-y-4">
          <AlertCircle className="h-12 w-12 text-red-500 mx-auto" />
          <h3 className="font-bold text-slate-900 text-lg">Đã xảy ra lỗi</h3>
          <p className="text-slate-500 max-w-sm mx-auto">{error}</p>
          <Button onClick={() => fetchRequests()} variant="outline" className="rounded-xl">Thử lại</Button>
        </Card>
      ) : !data || data.content.length === 0 ? (
        <Card className="rounded-3xl border-none shadow-sm p-12 text-center space-y-4 bg-white">
          <FileText className="h-16 w-16 text-slate-300 mx-auto" />
          <h3 className="font-bold text-slate-700 text-lg">Không có yêu cầu nào</h3>
          <p className="text-slate-500 max-w-xs mx-auto">
            Hiện tại không có yêu cầu xem xét ngoại lệ hoàn tiền nào liên quan đến sân của bạn.
          </p>
        </Card>
      ) : (
        <div className="space-y-4">
          {data.content.map((req) => (
            <Card key={req.requestId} className="rounded-3xl border-none shadow-sm bg-white overflow-hidden hover:shadow-md transition-shadow">
              <CardContent className="p-6 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Yêu cầu #{req.requestId} · Đơn đặt #{req.bookingId}</span>
                    <h3 className="font-bold text-slate-800 text-base">
                      {req.stadiumName}
                    </h3>
                  </div>
                  <Badge variant="outline" className={`${EXCEPTION_STATUS_CONFIG[req.status as keyof typeof EXCEPTION_STATUS_CONFIG]?.className || "bg-slate-50 text-slate-600"} font-bold px-3 py-1 rounded-full text-[10px] uppercase tracking-wider`}>
                    {EXCEPTION_STATUS_CONFIG[req.status as keyof typeof EXCEPTION_STATUS_CONFIG]?.label || req.status}
                  </Badge>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs font-medium text-slate-500 py-2 border-y border-slate-100">
                  <div className="flex items-center gap-2">
                    <User className="h-4 w-4 text-slate-400" />
                    <span>Khách hàng: {req.customerName}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-slate-400" />
                    <span>Ngày chơi: {req.reservationDate}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4 text-slate-400" />
                    <span>Yêu cầu lúc: {new Date(req.createdAt).toLocaleString("vi-VN")}</span>
                  </div>
                </div>

                <div className="space-y-2 text-sm">
                  <p className="text-slate-600 leading-relaxed italic bg-slate-50/50 p-3 rounded-2xl border border-slate-100">
                    <span className="font-semibold text-slate-800 not-italic block mb-1">Trình bày lý do từ khách:</span>
                    "{req.reason}"
                  </p>

                  {req.evidenceUrl && (
                    <div className="space-y-1.5">
                      <span className="text-xs font-semibold text-slate-700">Bằng chứng khách gửi:</span>
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
                            // Không phải ảnh (vd link Google Drive cũ) -> ẩn ảnh, fallback về link text
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

                  {req.ownerNote && (
                    <p className="text-slate-600 leading-relaxed bg-slate-50 p-3 rounded-2xl">
                      <span className="font-semibold text-slate-800 block mb-0.5">Ghi chú phản hồi của bạn:</span>
                      "{req.ownerNote}"
                    </p>
                  )}
                </div>

                {req.status === "PENDING_OWNER" && (
                  <div className="flex justify-end gap-3 pt-2">
                    <Button
                      onClick={() => openReviewModal(req, false)}
                      variant="outline"
                      className="rounded-xl border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700 font-bold h-10 px-5 text-xs flex items-center gap-1.5"
                    >
                      <X className="h-4 w-4" />
                      Từ chối
                    </Button>
                    <Button
                      onClick={() => openReviewModal(req, true)}
                      className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold h-10 px-5 text-xs flex items-center gap-1.5"
                    >
                      <Check className="h-4 w-4" />
                      Chấp nhận hoàn tiền
                    </Button>
                  </div>
                )}
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
                  onClick={() => fetchRequests(idx)}
                >
                  {idx + 1}
                </Button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Review Dialog */}
      <Dialog open={selectedRequest !== null} onOpenChange={(open) => !open && setSelectedRequest(null)}>
        <DialogContent className="rounded-3xl sm:max-w-md p-6">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold">
              {approved ? "Chấp nhận yêu cầu ngoại lệ" : "Từ chối yêu cầu ngoại lệ"}
            </DialogTitle>
            <DialogDescription className="text-slate-500 font-medium">
              {approved
                ? "Duyệt hoàn trả một phần tiền cọc cho khách hàng do sự kiện bất khả kháng."
                : "Từ chối hoàn trả. Khách hàng có thể leo thang lên Ban quản trị."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 my-2">
            {approved && (
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Mức phần trăm hoàn trả
                </label>
                <Select value={refundPercent} onValueChange={setRefundPercent}>
                  <SelectTrigger className="rounded-xl border-slate-200">
                    <SelectValue placeholder="Chọn tỷ lệ..." />
                  </SelectTrigger>
                  <SelectContent className="rounded-xl">
                    <SelectItem value="50">Hoàn trả 50% số tiền</SelectItem>
                    <SelectItem value="100">Hoàn trả 100% số tiền</SelectItem>
                  </SelectContent>
                </Select>
                <div className="rounded-lg bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700 flex gap-2">
                  <Info className="h-4 w-4 shrink-0" />
                  <span>Theo quy định, chỉ cho phép hoàn 50% hoặc 100% khi duyệt chấp nhận.</span>
                </div>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Ghi chú phản hồi <span className="text-slate-400">(Tùy chọn)</span>
              </label>
              <Textarea
                placeholder={approved ? "Lý do chấp nhận hoàn tiền..." : "Lý do từ chối yêu cầu..."}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className="min-h-[100px] rounded-2xl border-slate-200 focus-visible:ring-primary text-sm"
              />
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="ghost" onClick={() => setSelectedRequest(null)} className="rounded-xl flex-1 border-slate-200">
              Hủy
            </Button>
            <Button
              onClick={handleReviewSubmit}
              disabled={submitting}
              className={`rounded-xl flex-1 font-bold ${
                approved ? "bg-emerald-600 hover:bg-emerald-700 text-white" : "bg-red-600 hover:bg-red-700 text-white"
              }`}
            >
              {submitting ? "Đang xử lý..." : approved ? "Xác nhận duyệt" : "Xác nhận từ chối"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
