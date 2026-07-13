"use client";

import { useMemo, useState } from "react";
import { AlertTriangle } from "lucide-react";
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
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  createUserReport,
  type ReportCategory,
} from "@/lib/services/user-report";

const CATEGORY_OPTIONS: Array<{ value: ReportCategory; label: string }> = [
  { value: "NO_SHOW", label: "Không xuất hiện / bỏ kèo" },
  { value: "PROPERTY_DAMAGE", label: "Phá hoại tài sản" },
  { value: "HARASSMENT", label: "Quấy rối / xúc phạm" },
  { value: "FRAUD", label: "Gian lận / lừa đảo" },
  { value: "PAYMENT_ABUSE", label: "Lạm dụng thanh toán" },
  { value: "FAKE_LISTING", label: "Thông tin sân giả/sai lệch" },
  { value: "OTHER", label: "Khác" },
];

type ReportUserDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  reporteeId?: number;
  bookingId?: number;
  stadiumId?: number;
  contextLabel?: string;
};

export function ReportUserDialog({
  open,
  onOpenChange,
  reporteeId,
  bookingId,
  stadiumId,
  contextLabel,
}: ReportUserDialogProps) {
  const [category, setCategory] = useState<ReportCategory>("HARASSMENT");
  const [description, setDescription] = useState("");
  const [evidenceText, setEvidenceText] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const evidenceUrls = useMemo(
    () => evidenceText
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .slice(0, 5),
    [evidenceText]
  );

  const handleSubmit = async () => {
    if (!reporteeId) {
      toast.error("Không tìm thấy tài khoản cần báo cáo.");
      return;
    }
    if (!description.trim()) {
      toast.error("Vui lòng nhập nội dung báo cáo.");
      return;
    }

    try {
      setSubmitting(true);
      await createUserReport({
        reporteeId,
        bookingId,
        stadiumId,
        category,
        description: description.trim(),
        evidenceUrls,
      });
      toast.success("Đã gửi báo cáo. Admin sẽ xem xét và xử lý.");
      setDescription("");
      setEvidenceText("");
      setCategory("HARASSMENT");
      onOpenChange(false);
    } catch (error: any) {
      toast.error(error.message || "Không thể gửi báo cáo.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-3xl sm:max-w-lg p-6">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl font-bold">
            <AlertTriangle className="h-5 w-5 text-amber-500" />
            Báo cáo hành vi
          </DialogTitle>
          <DialogDescription className="text-slate-500 font-medium">
            {contextLabel || "Báo cáo này sẽ được gửi cho Admin xem xét."}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="report-category">Loại báo cáo</Label>
            <select
              id="report-category"
              value={category}
              onChange={(event) => setCategory(event.target.value as ReportCategory)}
              className="h-11 w-full rounded-2xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-800 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            >
              {CATEGORY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="report-description">Nội dung</Label>
            <Textarea
              id="report-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Mô tả hành vi vi phạm, thời điểm xảy ra, và thông tin Admin cần biết..."
              className="min-h-[120px] rounded-2xl border-slate-200 focus-visible:ring-primary"
              maxLength={2000}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="report-evidence">Bằng chứng (mỗi dòng một URL, tối đa 5)</Label>
            <Textarea
              id="report-evidence"
              value={evidenceText}
              onChange={(event) => setEvidenceText(event.target.value)}
              placeholder="https://..."
              className="min-h-[88px] rounded-2xl border-slate-200 focus-visible:ring-primary"
            />
          </div>
        </div>

        <DialogFooter className="flex-col sm:flex-row gap-2">
          <Button variant="ghost" onClick={() => onOpenChange(false)} className="rounded-xl flex-1">
            Hủy
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!description.trim() || submitting}
            className="rounded-xl flex-1 bg-amber-600 hover:bg-amber-700 font-bold"
          >
            {submitting ? "Đang gửi..." : "Gửi báo cáo"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
