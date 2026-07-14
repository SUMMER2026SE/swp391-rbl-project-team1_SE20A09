"use client";

import { useEffect, useState } from "react";
import { AlertTriangle, Loader2, ShieldAlert } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { getMyReports, type MyReport, type ReportCategory, type ReportStatus } from "@/lib/services/user-report";

const CATEGORY_LABELS: Record<ReportCategory, string> = {
  NO_SHOW: "Không xuất hiện / bỏ kèo",
  PROPERTY_DAMAGE: "Phá hoại tài sản",
  HARASSMENT: "Quấy rối / xúc phạm",
  FRAUD: "Gian lận / lừa đảo",
  PAYMENT_ABUSE: "Lạm dụng thanh toán",
  FAKE_LISTING: "Thông tin sân giả/sai lệch",
  OTHER: "Khác",
};

function statusBadge(status: ReportStatus) {
  if (status === "ACTION_TAKEN") return <Badge className="bg-emerald-600">Đã xử lý</Badge>;
  if (status === "DISMISSED") return <Badge variant="secondary">Đã bác bỏ</Badge>;
  if (status === "UNDER_REVIEW") return <Badge className="bg-amber-500">Đang xem xét</Badge>;
  return <Badge className="bg-rose-600">Mới</Badge>;
}

type MyReportsListProps = {
  showHeading?: boolean;
};

export function MyReportsList({ showHeading = true }: MyReportsListProps) {
  const [reports, setReports] = useState<MyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getMyReports();
        setReports(data.content ?? []);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Không thể tải danh sách báo cáo.");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <div>
      {showHeading && (
        <div className="mb-8">
          <h1 className="text-3xl font-bold flex items-center gap-3">
            <AlertTriangle className="h-7 w-7 text-amber-500" />
            Báo cáo của tôi
          </h1>
          <p className="text-muted-foreground mt-2">
            Danh sách các báo cáo hành vi bạn đã gửi và trạng thái xử lý từ Admin.
          </p>
        </div>
      )}

      {error && <div className="rounded-md bg-rose-50 p-3 text-sm text-rose-700 mb-4">{error}</div>}

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : reports.length === 0 ? (
        <div className="rounded-lg border border-slate-200 bg-white py-14 text-center text-slate-500">
          <ShieldAlert className="mx-auto mb-3 h-10 w-10 text-slate-300" />
          Bạn chưa gửi báo cáo hành vi nào.
        </div>
      ) : (
        <div className="space-y-4">
          {reports.map((report) => (
            <Card key={report.reportId}>
              <CardContent className="space-y-3 p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="font-semibold text-slate-800">
                      Báo cáo {report.reportee.fullName} ({report.reportee.roleName})
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-slate-400">
                      <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
                      {CATEGORY_LABELS[report.category]} · {new Date(report.createdAt).toLocaleString("vi-VN")}
                    </div>
                  </div>
                  {statusBadge(report.status)}
                </div>

                <p className="text-sm text-slate-600 whitespace-pre-wrap">{report.description}</p>

                {(report.status === "ACTION_TAKEN" || report.status === "DISMISSED") && (
                  <div className="rounded-md bg-slate-50 p-3 text-sm">
                    <div className="font-medium text-slate-700">Kết quả xử lý</div>
                    <p className="mt-1 text-slate-600">
                      {report.resolutionNote || "Admin đã xử lý báo cáo này, không có ghi chú thêm."}
                    </p>
                    {report.resolvedAt && (
                      <p className="mt-1 text-xs text-slate-400">
                        {new Date(report.resolvedAt).toLocaleString("vi-VN")}
                      </p>
                    )}
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
