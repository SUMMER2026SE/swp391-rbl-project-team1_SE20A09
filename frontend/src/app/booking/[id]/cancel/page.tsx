'use client'

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { AlertCircle, ArrowLeft, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { put } from "@/lib/api";
import { fetchBookingDetail, fetchRefundPreview, type RefundPreview } from "@/lib/bookings-api";

export default function CancelBookingPage() {
  const params = useParams();
  const router = useRouter();
  const bookingId = params?.id as string;

  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [paymentStatus, setPaymentStatus] = useState<string | null>(null);
  const [refundPreview, setRefundPreview] = useState<RefundPreview | null>(null);

  // UC-CUS-06: Nếu booking đã thanh toán → gọi preview API để hiển thị số tiền hoàn.
  useEffect(() => {
    if (!bookingId) return;
    let cancelled = false;
    (async () => {
      try {
        const detail = await fetchBookingDetail(bookingId);
        if (cancelled) return;
        const ps = (detail.paymentStatus ?? "").toLowerCase();
        setPaymentStatus(ps);

        if (ps === "paid") {
          setLoadingPreview(true);
          try {
            const preview = await fetchRefundPreview(bookingId);
            if (!cancelled) setRefundPreview(preview);
          } catch (err: any) {
            // Không block flow huỷ — chỉ ẩn preview.
            if (!cancelled) {
              setRefundPreview(null);
              console.warn("fetchRefundPreview failed:", err?.message);
            }
          } finally {
            if (!cancelled) setLoadingPreview(false);
          }
        }
      } catch (err: any) {
        // detail fail — vẫn cho phép user thử huỷ.
        console.warn("fetchBookingDetail failed:", err?.message);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [bookingId]);

  const handleCancel = async () => {
    if (!reason.trim()) {
      toast.error("Vui lòng nhập lý do huỷ đặt sân");
      return;
    }

    try {
      setSubmitting(true);
      await put(`/bookings/${bookingId}/cancel`, { reason });
      const msg = paymentStatus === "paid" && refundPreview && refundPreview.refundAmount > 0
        ? "Huỷ đặt sân thành công. Yêu cầu hoàn tiền đã được gửi tới chủ sân."
        : "Huỷ đặt sân thành công";
      toast.success(msg);
      router.refresh();
      router.push("/profile?tab=bookings");
    } catch (err: any) {
      toast.error(err.message || "Có lỗi xảy ra khi huỷ đặt sân");
    } finally {
      setSubmitting(false);
    }
  };

  const formatVnd = (amount: number) => `${amount.toLocaleString("vi-VN")}đ`;
  const showRefundPanel = paymentStatus === "paid";

  return (
    <div className="min-h-screen bg-muted/30 p-4 sm:p-8 flex justify-center items-center">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center gap-2 mb-2">
            <Link href={`/booking/${bookingId}`}>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <CardTitle>Xác nhận huỷ đặt sân</CardTitle>
          </div>
          <CardDescription>
            Bạn đang yêu cầu huỷ đơn đặt sân #{bookingId}. Vui lòng cho chúng tôi biết lý do.
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="rounded-md bg-yellow-50 p-4 border border-yellow-200">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertCircle className="h-5 w-5 text-yellow-600" aria-hidden="true" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-yellow-800">Lưu ý về chính sách hoàn tiền</h3>
                <div className="mt-2 text-sm text-yellow-700">
                  <p>
                    - Huỷ trước 24h: Hoàn 100% tiền cọc.
                    <br />
                    - Huỷ trước 12h: Hoàn 50% tiền cọc.
                    <br />
                    - Huỷ dưới 12h: Không hoàn tiền cọc.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {showRefundPanel && (
            <div className="rounded-md bg-emerald-50 p-4 border border-emerald-200">
              <h3 className="text-sm font-semibold text-emerald-800 mb-2">
                Số tiền hoàn dự kiến
              </h3>
              {loadingPreview ? (
                <div className="flex items-center gap-2 text-sm text-emerald-700">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang tính toán...
                </div>
              ) : refundPreview ? (
                <div className="space-y-1.5 text-sm">
                  <div className="flex justify-between">
                    <span className="text-emerald-700">Số tiền đã thanh toán:</span>
                    <span className="font-medium text-emerald-900">
                      {formatVnd(refundPreview.originalAmount)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-emerald-700">Số tiền hoàn:</span>
                    <span className="font-bold text-emerald-900">
                      {formatVnd(refundPreview.refundAmount)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-emerald-700">Phần trăm hoàn:</span>
                    <span className="font-medium text-emerald-900">
                      {refundPreview.refundPercent}%
                    </span>
                  </div>
                  <p className="text-xs text-emerald-700 mt-2 italic">
                    Hệ thống sẽ gửi yêu cầu tới Chủ sân xử lý. Vui lòng chờ phản hồi.
                  </p>
                </div>
              ) : (
                <p className="text-sm text-emerald-700">
                  Không thể tính số tiền hoàn. Bạn vẫn có thể huỷ — chủ sân sẽ xem xét yêu cầu.
                </p>
              )}
            </div>
          )}

          <div className="rounded-md bg-red-50 p-3 border border-red-200">
            <p className="text-sm font-medium text-red-700">
              ⚠️ Hành động này không thể hoàn tác.
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="reason">Lý do huỷ (Bắt buộc) *</Label>
            <Textarea
              id="reason"
              placeholder="VD: Thay đổi lịch trình, thời tiết xấu..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required
              rows={4}
            />
          </div>
        </CardContent>

        <CardFooter className="flex gap-2">
          <Link href={`/booking/${bookingId}`} className="flex-1">
            <Button variant="outline" className="w-full">
              Trở lại
            </Button>
          </Link>
          <Button
            variant="destructive"
            className="flex-1"
            onClick={handleCancel}
            disabled={submitting || !reason.trim()}
          >
            {submitting ? "Đang xử lý..." : "Xác nhận huỷ"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}