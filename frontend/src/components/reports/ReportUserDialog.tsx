"use client";

import { useRef, useState } from "react";
import { AlertTriangle, Loader2, UploadCloud, X } from "lucide-react";
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
import { uploadDocument } from "@/lib/api";
import {
  createUserReport,
  type ReportCategory,
} from "@/lib/services/user-report";

const MAX_EVIDENCE_FILES = 5;
const MAX_EVIDENCE_FILE_SIZE = 5 * 1024 * 1024;

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
  matchRequestId?: number;
  joinRequestId?: number;
  stadiumId?: number;
  contextLabel?: string;
};

export function ReportUserDialog({
  open,
  onOpenChange,
  reporteeId,
  bookingId,
  matchRequestId,
  joinRequestId,
  stadiumId,
  contextLabel,
}: ReportUserDialogProps) {
  const [category, setCategory] = useState<ReportCategory>("HARASSMENT");
  const [description, setDescription] = useState("");
  const [evidenceUrls, setEvidenceUrls] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFilesSelected = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (files.length === 0) return;

    const remainingSlots = MAX_EVIDENCE_FILES - evidenceUrls.length;
    if (remainingSlots <= 0) return;

    const toUpload = files.slice(0, remainingSlots);
    const oversized = toUpload.find((file) => file.size > MAX_EVIDENCE_FILE_SIZE);
    if (oversized) {
      toast.error(`Ảnh "${oversized.name}" vượt quá 5MB.`);
      return;
    }

    setUploading(true);
    try {
      const uploaded = await Promise.all(toUpload.map((file) => uploadDocument(file)));
      setEvidenceUrls((current) => [...current, ...uploaded.map((result) => result.url)]);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Không thể tải ảnh lên.");
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveEvidence = (url: string) => {
    setEvidenceUrls((current) => current.filter((item) => item !== url));
  };

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
        matchRequestId,
        joinRequestId,
        stadiumId,
        category,
        description: description.trim(),
        evidenceUrls,
      });
      toast.success("Đã gửi báo cáo. Admin sẽ xem xét và xử lý.");
      setDescription("");
      setEvidenceUrls([]);
      setCategory("HARASSMENT");
      onOpenChange(false);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Không thể gửi báo cáo.");
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
            <Label>Bằng chứng (ảnh chụp màn hình, tối đa {MAX_EVIDENCE_FILES})</Label>
            <div className="grid grid-cols-3 gap-2">
              {evidenceUrls.map((url) => (
                <div
                  key={url}
                  className="relative aspect-square rounded-xl overflow-hidden border border-slate-200 bg-slate-50 group"
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img src={url} alt="Bằng chứng" className="h-full w-full object-cover" />
                  <button
                    type="button"
                    onClick={() => handleRemoveEvidence(url)}
                    className="absolute top-1 right-1 h-6 w-6 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                    aria-label="Xoá ảnh bằng chứng"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
              ))}

              {evidenceUrls.length < MAX_EVIDENCE_FILES && (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading}
                  className="aspect-square rounded-xl border-2 border-dashed border-slate-200 hover:border-primary/50 hover:bg-primary/5 flex flex-col items-center justify-center gap-1 text-slate-400 transition-colors disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {uploading ? (
                    <Loader2 className="h-5 w-5 animate-spin" />
                  ) : (
                    <>
                      <UploadCloud className="h-5 w-5" />
                      <span className="text-[11px] font-medium">Tải ảnh</span>
                    </>
                  )}
                </button>
              )}
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              className="hidden"
              onChange={handleFilesSelected}
            />
            <p className="text-[11px] text-slate-400">JPG, PNG, WEBP, GIF hoặc BMP · Tối đa 5MB/ảnh.</p>
          </div>
        </div>

        <DialogFooter className="flex-col sm:flex-row gap-2">
          <Button variant="ghost" onClick={() => onOpenChange(false)} className="rounded-xl flex-1">
            Hủy
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!description.trim() || submitting || uploading}
            className="rounded-xl flex-1 bg-amber-600 hover:bg-amber-700 font-bold"
          >
            {submitting ? "Đang gửi..." : "Gửi báo cáo"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
