"use client";

import { useState } from "react";
import { Loader2, HelpCircle } from "lucide-react";
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
import { post } from "@/lib/api";
import { DocumentUploader } from "@/components/shared/DocumentUploader";

interface RefundExceptionDialogProps {
  /** Id của booking cần xin xem xét ngoại lệ hoàn tiền — null khi đóng. */
  bookingId: number | null;
  /** Callback khi dialog đóng. */
  onOpenChange: (open: boolean) => void;
  /** Callback khi submit thành công. */
  onSubmitSuccess?: () => void;
}

export function RefundExceptionDialog({
  bookingId,
  onOpenChange,
  onSubmitSuccess,
}: RefundExceptionDialogProps) {
  const [reason, setReason] = useState("");
  const [evidenceUrl, setEvidenceUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const open = bookingId !== null;

  const handleOpenChange = (next: boolean) => {
    if (!next) {
      setReason("");
      setEvidenceUrl("");
      setSubmitting(false);
    }
    onOpenChange(next);
  };

  const handleConfirm = async () => {
    if (bookingId === null) return;
    if (reason.trim().length < 5) {
      toast.error("Lý do xin xem xét phải dài tối thiểu 5 ký tự");
      return;
    }

    setSubmitting(true);
    try {
      await post("/refund-exceptions", {
        bookingId,
        reason: reason.trim(),
        evidenceUrl: evidenceUrl.trim() || null,
      });
      toast.success("Gửi yêu cầu ngoại lệ thành công! Chủ sân sẽ phản hồi.");
      onSubmitSuccess?.();
      handleOpenChange(false);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Không thể gửi yêu cầu, vui lòng thử lại sau";
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="rounded-2xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <HelpCircle className="h-5 w-5 text-emerald-500" />
            Yêu cầu xem xét ngoại lệ hoàn tiền
          </DialogTitle>
          <DialogDescription className="text-sm text-slate-600">
            Xin hoàn tiền do các lý do bất khả kháng (sức khỏe, thiên tai, tai nạn...). Quyết định cuối cùng thuộc về Chủ sân và Ban quản trị.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 my-2">
          <div className="space-y-1.5">
            <label
              htmlFor="exception-reason"
              className="text-xs font-semibold text-slate-700 uppercase tracking-wider"
            >
              Lý do bất khả kháng <span className="text-red-500">*</span>
            </label>
            <textarea
              id="exception-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Vui lòng trình bày rõ hoàn cảnh bất khả kháng khiến bạn không thể tham gia chơi..."
              rows={4}
              maxLength={2000}
              className="w-full resize-none rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 placeholder:text-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 disabled:opacity-60"
              disabled={submitting}
            />
            <p className="text-[10px] text-slate-400 text-right">
              {reason.length}/2000 ký tự (tối thiểu 5 ký tự)
            </p>
          </div>

          <DocumentUploader
            label="Bằng chứng đính kèm (Tùy chọn)"
            value={evidenceUrl}
            onChange={setEvidenceUrl}
            disabled={submitting}
          />
        </div>

        <DialogFooter className="gap-2 sm:gap-2">
          <Button
            type="button"
            variant="outline"
            className="rounded-xl border-slate-200"
            onClick={() => handleOpenChange(false)}
            disabled={submitting}
          >
            Quay lại
          </Button>
          <Button
            type="button"
            className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white"
            onClick={handleConfirm}
            disabled={submitting}
          >
            {submitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang gửi...
              </>
            ) : (
              "Gửi yêu cầu"
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
