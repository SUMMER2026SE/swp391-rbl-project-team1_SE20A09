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
  evidenceUrl: string | null;
  status: string;
  ownerNote: string | null;
  adminNote: string | null;
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

const STATUS_CONFIG = {
  PENDING_ADMIN: { label: "Chờ Admin duyệt", className: "bg-indigo-50 text-indigo-700 border-indigo-200" },
  APPROVED_ADMIN: { label: "Đã hoàn tiền", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
  REJECTED_ADMIN: { label: "Từ chối cuối", className: "bg-slate-50 text-slate-700 border-slate-200" },
  PENDING_OWNER: { label: "Chờ Owner", className: "bg-amber-50 text-amber-700 border-amber-200" },
  APPROVED_OWNER: { label: "Owner duyệt", className: "bg-green-50 text-green-700 border-green-200" },
  REJECTED_OWNER: { label: "Owner từ chối", className: "bg-red-50 text-red-700 border-red-200" },
} as const;

export default function AdminRefundExceptionsPage() {
  const [data, setData] = useState<PageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  // Review dialog
  const [selected, setSelected] = useState<RefundException | null>(null);
  const [approved, setApproved] = useState<boolean | null>(null);
  const [refundPercent, setRefundPercent] = useState<string>("50");
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const fetchQueue = async (pageNum = 0) => {
    try {
      setLoading(true);
      const res = await get<PageResponse>(`/admin/refund-exceptions?page=${pageNum}&size=20`);
      setData(res);
      setPage(pageNum);
    } catch (err: any) {
      setError(err.message || "Không thể tải danh sách yêu cầu.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, []);

  const openModal = (req: RefundException, approve: boolean) => {
    setSelected(req);
    setApproved(approve);
    setRefundPercent("50");
    setNote("");
  };

  const handleSubmit = async () => {
    if (!selected || approved === null) return;
    setSubmitting(true);
    try {
      await put(`/admin/refund-exceptions/${selected.requestId}/decide`, {
        approved,
        refundPercent: approved ? parseInt(refundPercent, 10) : null,
        adminNote: note.trim() || null,
      });
      toast.success(approved ? "Đã phê duyệt hoàn tiền thành công." : "Đã từ chối yêu cầu ngoại lệ.");
      setSelected(null);
      fetchQueue(page);
    } catch (err: any) {
      toast.error(err.message || "Có lỗi xảy ra khi cập nhật.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Hàng chờ xét duyệt ngoại lệ</h2>
          <p className="text-sm text-slate-500 mt-1">
            Các yêu cầu hoàn tiền bất khả kháng được Owner từ chối và khách leo thang lên Admin.
          </p>
        </div>
        {data && (
          <div className="text-right">
            <span className="text-2xl font-bold text-indigo-600">{data.totalElements}</span>
            <p className="text-xs text-slate-500 font-medium">yêu cầu chờ xử lý</p>
          </div>
        )}
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
          <Button onClick={() => fetchQueue()} variant="outline" className="rounded-xl">Thử lại</Button>
        </Card>
      ) : !data || data.content.length === 0 ? (
        <Card className="rounded-3xl border-none shadow-sm p-12 text-center space-y-4 bg-white">
          <FileText className="h-16 w-16 text-slate-300 mx-auto" />
          <h3 className="font-bold text-slate-700 text-lg">Không có yêu cầu nào</h3>
          <p className="text-slate-500 max-w-xs mx-auto">Hàng chờ xét duyệt của Admin hiện đang trống.</p>
        </Card>
      ) : (
        <div className="space-y-4">
          {data.content.map((req) => (
            <Card key={req.requestId} className="rounded-3xl border-none shadow-sm bg-white overflow-hidden hover:shadow-md transition-shadow">
              <CardContent className="p-6 space-y-4">
                {/* Header */}
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                      Yêu cầu #{req.requestId} · Booking #{req.bookingId}
                    </span>
                    <h3 className="font-bold text-slate-800 text-base">{req.stadiumName}</h3>
                  </div>
                  <Badge
                    variant="outline"
                    className={`${STATUS_CONFIG[req.status as keyof typeof STATUS_CONFIG]?.className || "bg-slate-50 text-slate-600"} font-bold px-3 py-1 rounded-full text-[10px] uppercase tracking-wider`}
                  >
                    {STATUS_CONFIG[req.status as keyof typeof STATUS_CONFIG]?.label || req.status}
                  </Badge>
                </div>

                {/* Meta */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs font-medium text-slate-500 py-2 border-y border-slate-100">
                  <div className="flex items-center gap-1.5">
                    <User className="h-3.5 w-3.5 text-slate-400" />
                    {req.customerName}
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5 text-slate-400" />
                    Ngày chơi: {req.reservationDate}
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Clock className="h-3.5 w-3.5 text-slate-400" />
                    Gửi: {new Date(req.createdAt).toLocaleString("vi-VN")}
                  </div>
                </div>

                {/* Content */}
                <div className="space-y-2 text-sm">
                  <div className="rounded-2xl bg-slate-50 p-4 space-y-3">
                    <div>
                      <span className="font-semibold text-slate-800 block mb-1">Lý do bất khả kháng của khách:</span>
                      <p className="text-slate-600 italic">"{req.reason}"</p>
                    </div>

                    {req.evidenceUrl && (
                      <div className="space-y-1.5">
                        <span className="text-xs font-semibold text-slate-700">Bằng chứng:</span>
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

                    {req.ownerNote && (
                      <div>
                        <span className="font-semibold text-slate-700">Ghi chú từ Owner:</span>
                        <p className="text-slate-600 mt-0.5">"{req.ownerNote}"</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Actions */}
                {req.status === "PENDING_ADMIN" && (
                  <div className="flex justify-end gap-3 pt-2">
                    <Button
                      onClick={() => openModal(req, false)}
                      variant="outline"
                      className="rounded-xl border-red-200 text-red-600 hover:bg-red-50 font-bold h-10 px-5 text-xs flex items-center gap-1.5"
                    >
                      <X className="h-4 w-4" />
                      Từ chối cuối
                    </Button>
                    <Button
                      onClick={() => openModal(req, true)}
                      className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold h-10 px-5 text-xs flex items-center gap-1.5"
                    >
                      <Check className="h-4 w-4" />
                      Phê duyệt hoàn tiền
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
                  onClick={() => fetchQueue(idx)}
                >
                  {idx + 1}
                </Button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Decide Dialog */}
      <Dialog open={selected !== null} onOpenChange={(open) => !open && setSelected(null)}>
        <DialogContent className="rounded-3xl sm:max-w-md p-6">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold">
              {approved ? "Phê duyệt hoàn tiền (Quyết định cuối)" : "Từ chối yêu cầu ngoại lệ (Cuối)"}
            </DialogTitle>
            <DialogDescription className="text-slate-500 font-medium">
              {approved
                ? "Quyết định của Admin là cuối cùng. Hệ thống sẽ kích hoạt hoàn tiền qua cổng thanh toán."
                : "Từ chối. Khách hàng sẽ không nhận được tiền hoàn và không thể kháng cáo tiếp."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 my-2">
            {approved && (
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Tỷ lệ hoàn trả
                </label>
                <Select value={refundPercent} onValueChange={setRefundPercent}>
                  <SelectTrigger className="rounded-xl border-slate-200">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="rounded-xl">
                    <SelectItem value="50">Hoàn 50% số tiền đã thu</SelectItem>
                    <SelectItem value="100">Hoàn 100% số tiền đã thu</SelectItem>
                  </SelectContent>
                </Select>
                <div className="rounded-lg bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700 flex gap-2">
                  <Info className="h-4 w-4 shrink-0" />
                  <span>Quy định: chỉ cho phép hoàn 50% hoặc 100% khi phê duyệt.</span>
                </div>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Căn cứ quyết định <span className="text-slate-400">(Tùy chọn)</span>
              </label>
              <Textarea
                placeholder={
                  approved
                    ? "Ví dụ: Hồ sơ bệnh án/tai nạn hợp lệ, chấp thuận hoàn tiền..."
                    : "Ví dụ: Thiếu bằng chứng xác thực, không đủ căn cứ..."
                }
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className="min-h-[100px] rounded-2xl border-slate-200 focus-visible:ring-primary text-sm"
              />
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="ghost"
              onClick={() => setSelected(null)}
              className="rounded-xl flex-1 border-slate-200"
            >
              Hủy
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={submitting}
              className={`rounded-xl flex-1 font-bold ${
                approved
                  ? "bg-emerald-600 hover:bg-emerald-700 text-white"
                  : "bg-red-600 hover:bg-red-700 text-white"
              }`}
            >
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xử lý...
                </>
              ) : approved ? (
                "Xác nhận phê duyệt"
              ) : (
                "Xác nhận từ chối"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
