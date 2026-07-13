"use client";

import { useEffect, useState } from "react";
import { AlertTriangle, CheckCircle2, Loader2, ShieldAlert, XCircle } from "lucide-react";
import api from "@/lib/api";
import type { PageResponse } from "@/types/common";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type ReportStatus = "OPEN" | "UNDER_REVIEW" | "ACTION_TAKEN" | "DISMISSED";
type ReportCategory =
  | "NO_SHOW"
  | "PROPERTY_DAMAGE"
  | "HARASSMENT"
  | "FRAUD"
  | "PAYMENT_ABUSE"
  | "FAKE_LISTING"
  | "OTHER";

type UserSummary = {
  userId: number;
  fullName: string;
  email: string;
  roleName: string;
};

type Report = {
  reportId: number;
  reporter: UserSummary;
  reportee: UserSummary;
  bookingId?: number;
  matchRequestId?: number;
  joinRequestId?: number;
  stadiumId?: number;
  stadiumName?: string;
  category: ReportCategory;
  description: string;
  evidenceUrls: string[];
  status: ReportStatus;
  resolvedBy?: UserSummary;
  resolvedAt?: string;
  resolutionNote?: string;
  createdAt: string;
};

const STATUS_OPTIONS: Array<{ value: ReportStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "Tất cả" },
  { value: "OPEN", label: "Mới" },
  { value: "UNDER_REVIEW", label: "Đang xem xét" },
  { value: "ACTION_TAKEN", label: "Đã xử lý" },
  { value: "DISMISSED", label: "Đã bác bỏ" },
];

const CATEGORY_OPTIONS: Array<{ value: ReportCategory | "ALL"; label: string }> = [
  { value: "ALL", label: "Tất cả loại" },
  { value: "NO_SHOW", label: "Không xuất hiện / bỏ kèo" },
  { value: "PROPERTY_DAMAGE", label: "Phá hoại tài sản" },
  { value: "HARASSMENT", label: "Quấy rối / xúc phạm" },
  { value: "FRAUD", label: "Gian lận / lừa đảo" },
  { value: "PAYMENT_ABUSE", label: "Lạm dụng thanh toán" },
  { value: "FAKE_LISTING", label: "Thông tin sân giả/sai lệch" },
  { value: "OTHER", label: "Khác" },
];

function categoryLabel(category: ReportCategory) {
  return CATEGORY_OPTIONS.find((option) => option.value === category)?.label || category;
}

function statusBadge(status: ReportStatus) {
  if (status === "ACTION_TAKEN") return <Badge className="bg-emerald-600">Đã xử lý</Badge>;
  if (status === "DISMISSED") return <Badge variant="secondary">Đã bác bỏ</Badge>;
  if (status === "UNDER_REVIEW") return <Badge className="bg-amber-500">Đang xem xét</Badge>;
  return <Badge className="bg-rose-600">Mới</Badge>;
}

function contextLabel(report: Report) {
  const parts: string[] = [];
  if (report.bookingId) parts.push(`Đơn đặt sân #${report.bookingId}`);
  if (report.matchRequestId) parts.push(`Kèo ghép #${report.matchRequestId}`);
  if (report.joinRequestId) parts.push(`Đơn tham gia #${report.joinRequestId}`);
  if (report.stadiumName) parts.push(`Sân: ${report.stadiumName}`);
  return parts.length > 0 ? parts.join(" · ") : "Không có ngữ cảnh cụ thể";
}

export default function AdminReportsPage() {
  const [status, setStatus] = useState<ReportStatus | "ALL">("OPEN");
  const [category, setCategory] = useState<ReportCategory | "ALL">("ALL");
  const [reports, setReports] = useState<Report[]>([]);
  const [notes, setNotes] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadReports = async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<PageResponse<Report>>("/admin/reports", {
        params: {
          status: status === "ALL" ? undefined : status,
          category: category === "ALL" ? undefined : category,
          page: 0,
          size: 20,
        },
      });
      setReports(data.content ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tải danh sách báo cáo.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, category]);

  const resolveReport = async (reportId: number, nextStatus: "UNDER_REVIEW" | "ACTION_TAKEN" | "DISMISSED") => {
    setResolvingId(reportId);
    setError(null);
    try {
      await api.patch(`/admin/reports/${reportId}`, {
        status: nextStatus,
        resolutionNote: notes[reportId]?.trim() || undefined,
      });
      await loadReports();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể xử lý báo cáo.");
    } finally {
      setResolvingId(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex flex-wrap rounded-md border border-slate-200 bg-white p-1">
          {STATUS_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => setStatus(option.value)}
              className={`rounded px-3 py-1.5 text-sm font-medium transition-colors ${
                status === option.value ? "bg-emerald-600 text-white" : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>

        <Select value={category} onValueChange={(value) => setCategory(value as ReportCategory | "ALL")}>
          <SelectTrigger className="w-56">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {CATEGORY_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {error && <div className="rounded-md bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="h-8 w-8 animate-spin text-emerald-600" />
        </div>
      ) : reports.length === 0 ? (
        <div className="rounded-lg border border-slate-200 bg-white py-14 text-center text-slate-500">
          <ShieldAlert className="mx-auto mb-3 h-10 w-10 text-slate-300" />
          Không có báo cáo nào
        </div>
      ) : (
        <div className="space-y-4">
          {reports.map((report) => (
            <Card key={report.reportId}>
              <CardContent className="space-y-4 p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2 text-sm text-slate-500">
                      <span className="font-semibold text-slate-800">{report.reporter.fullName}</span>
                      <span className="text-slate-400">({report.reporter.roleName})</span>
                      <span>báo cáo</span>
                      <span className="font-semibold text-slate-800">{report.reportee.fullName}</span>
                      <span className="text-slate-400">({report.reportee.roleName})</span>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-slate-400">
                      <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
                      {categoryLabel(report.category)} · {new Date(report.createdAt).toLocaleString("vi-VN")}
                    </div>
                  </div>
                  {statusBadge(report.status)}
                </div>

                <div className="rounded-md bg-slate-50 p-3 text-sm">
                  <div className="font-medium text-slate-700">Nội dung báo cáo</div>
                  <p className="mt-1 whitespace-pre-wrap text-slate-600">{report.description}</p>
                </div>

                <div className="text-xs text-slate-500">{contextLabel(report)}</div>

                {report.evidenceUrls.length > 0 && (
                  <div className="space-y-1 text-sm">
                    <div className="font-medium text-slate-700">Bằng chứng</div>
                    {report.evidenceUrls.map((url) => (
                      <a
                        key={url}
                        href={url}
                        target="_blank"
                        rel="noreferrer"
                        className="block break-all text-emerald-700 hover:underline"
                      >
                        {url}
                      </a>
                    ))}
                  </div>
                )}

                {report.status !== "OPEN" && report.resolutionNote && (
                  <div className="rounded-md bg-slate-50 p-3 text-sm">
                    <div className="font-medium text-slate-700">Ghi chú xử lý</div>
                    <p className="mt-1 text-slate-600">{report.resolutionNote}</p>
                    {report.resolvedBy && (
                      <p className="mt-1 text-xs text-slate-400">
                        Bởi {report.resolvedBy.fullName}
                        {report.resolvedAt && ` · ${new Date(report.resolvedAt).toLocaleString("vi-VN")}`}
                      </p>
                    )}
                  </div>
                )}

                {(report.status === "OPEN" || report.status === "UNDER_REVIEW") && (
                  <div className="space-y-3">
                    <Textarea
                      value={notes[report.reportId] ?? ""}
                      onChange={(event) =>
                        setNotes((current) => ({ ...current, [report.reportId]: event.target.value }))
                      }
                      placeholder="Ghi chú xử lý (tuỳ chọn)"
                      rows={3}
                    />
                    <div className="flex flex-wrap gap-2">
                      {report.status === "OPEN" && (
                        <Button
                          variant="outline"
                          onClick={() => resolveReport(report.reportId, "UNDER_REVIEW")}
                          disabled={resolvingId === report.reportId}
                        >
                          Đánh dấu đang xem xét
                        </Button>
                      )}
                      <Button
                        onClick={() => resolveReport(report.reportId, "ACTION_TAKEN")}
                        disabled={resolvingId === report.reportId}
                        className="bg-emerald-600 hover:bg-emerald-700"
                      >
                        <CheckCircle2 className="mr-2 h-4 w-4" />
                        Xác nhận vi phạm
                      </Button>
                      <Button
                        variant="destructive"
                        onClick={() => resolveReport(report.reportId, "DISMISSED")}
                        disabled={resolvingId === report.reportId}
                      >
                        <XCircle className="mr-2 h-4 w-4" />
                        Bác bỏ
                      </Button>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
