"use client";

import { useState, useEffect } from "react";
import { Loader2, AlertCircle } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cancelBooking, previewRefund, RefundPreviewResponse } from "@/lib/bookings-api";

interface CancelBookingDialogProps {
  /** Id của booking cần hủy — null khi dialog đóng. */
  bookingId: number | null;
  /** Callback khi dialog đóng (sau khi hủy thành công hoặc bấm Quay lại). */
  onOpenChange: (open: boolean) => void;
  /** Callback khi hủy thành công — parent sẽ dùng để refetch danh sách. */
  onCancelled?: () => void;
}

export function CancelBookingDialog({
  bookingId,
  onOpenChange,
  onCancelled,
}: CancelBookingDialogProps) {
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [preview, setPreview] = useState<RefundPreviewResponse | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);

  const open = bookingId !== null;

  useEffect(() => {
    if (bookingId) {
      setLoadingPreview(true);
      previewRefund(bookingId)
        .then(setPreview)
        .catch(() => setPreview(null))
        .finally(() => setLoadingPreview(false));
    }
  }, [bookingId]);

  const handleOpenChange = (next: boolean) => {
    if (!next) {
      setReason("");
      setSubmitting(false);
      setPreview(null);
    }
    onOpenChange(next);
  };

  const handleConfirm = async () => {
    if (bookingId === null) return;
    setSubmitting(true);
    try {
      await cancelBooking(bookingId, reason.trim() || undefined);
      toast.success("Đã hủy đơn thành công");
      onCancelled?.();
      handleOpenChange(false);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Không thể hủy đơn, vui lòng thử lại";
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="rounded-2xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg font-bold text-slate-900">
            Xác nhận hủy đơn
          </DialogTitle>
          <DialogDescription className="text-sm text-slate-600">
            Bạn có chắc chắn muốn hủy đơn đặt sân này?
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <label
            htmlFor="cancel-reason"
            className="text-sm font-medium text-slate-700"
          >
            Lý do hủy
          </label>
          <textarea
            id="cancel-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Lý do hủy (không bắt buộc)"
            rows={3}
            maxLength={255}
            className="w-full resize-none rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 placeholder:text-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 disabled:opacity-60"
            disabled={submitting}
          />
          {loadingPreview ? (
            <div className="flex items-center justify-center py-2 text-sm text-slate-500">
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Đang tính toán số tiền hoàn...
            </div>
          ) : preview ? (
            <div className="rounded-lg bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700 flex gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <div className="space-y-1">
                <span>
                  {preview.refundAmount > 0
                    ? `Bạn sẽ được hoàn lại: ${preview.refundAmount.toLocaleString("vi-VN")} VND (${preview.refundPercentage}% số tiền thực thu).`
                    : "Đơn này sẽ không được hoàn tiền."}
                </span>
                {preview.cancellationPolicyDescription && (
                  <p className="text-[11px] font-normal text-amber-600/90 italic">
                    {preview.cancellationPolicyDescription}
                  </p>
                )}
              </div>
            </div>
          ) : (
            <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700">
              Đơn này chưa ghi nhận thanh toán qua cổng (vd đang chờ thu tiền mặt tại sân) — hủy sẽ không phát sinh hoàn tiền.
            </p>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-2">
          <Button
            type="button"
            variant="outline"
            className="rounded-xl"
            onClick={() => handleOpenChange(false)}
            disabled={submitting}
          >
            Quay lại
          </Button>
          <Button
            type="button"
            variant="destructive"
            className="rounded-xl"
            onClick={handleConfirm}
            disabled={submitting}
          >
            {submitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang xử lý...
              </>
            ) : (
              "Xác nhận hủy"
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
