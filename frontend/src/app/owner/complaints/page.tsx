'use client'

import { useState, useEffect, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  AlertTriangle, Send, Loader2, AlertCircle,
  RefreshCw, CheckCircle2, Clock, ShieldAlert
} from "lucide-react";
import { get, put } from "@/lib/api";

// ── Types ────────────────────────────────────────────────────
interface ComplainerInfo {
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
}

interface StadiumSummary {
  stadiumId: number;
  stadiumName: string;
}

interface ComplaintData {
  complaintId: number;
  bookingId: number;
  complainer: ComplainerInfo;
  stadium: StadiumSummary;
  content: string;
  status: string;
  response: string | null;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Component ────────────────────────────────────────────────
function ComplaintManagementPage() {
  const [complaints, setComplaints] = useState<ComplaintData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [responseText, setResponseText] = useState("");
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [filter, setFilter] = useState<string>("all");

  const fetchComplaints = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await get<PageResponse<ComplaintData>>("/owner/complaints?size=50");
      setComplaints(data.content);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Không thể tải khiếu nại";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  const handleResolve = async (complaintId: number) => {
    if (!responseText.trim()) {
      alert("Vui lòng nhập nội dung phản hồi.");
      return;
    }
    setProcessingId(complaintId);
    try {
      await put(`/owner/complaints/${complaintId}/resolve`, {
        response: responseText,
      });
      setResolvingId(null);
      setResponseText("");
      await fetchComplaints();
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Không thể giải quyết khiếu nại";
      alert(errorMessage);
    } finally {
      setProcessingId(null);
    }
  };

  const getStatusBadge = (status: string) => {
    const config: Record<string, { label: string; className: string; icon: React.ReactNode }> = {
      OPEN: {
        label: "Chưa xử lý",
        className: "bg-red-100 text-red-700",
        icon: <ShieldAlert className="h-3 w-3" />,
      },
      IN_PROGRESS: {
        label: "Đang xử lý",
        className: "bg-yellow-100 text-yellow-700",
        icon: <Clock className="h-3 w-3" />,
      },
      RESOLVED: {
        label: "Đã giải quyết",
        className: "bg-green-100 text-green-700",
        icon: <CheckCircle2 className="h-3 w-3" />,
      },
    };
    const item = config[status] || { label: status, className: "bg-gray-100", icon: null };
    return (
      <Badge className={`${item.className} flex items-center gap-1`}>
        {item.icon}
        {item.label}
      </Badge>
    );
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit", month: "2-digit", year: "numeric",
      hour: "2-digit", minute: "2-digit",
    });
  };

  const filteredComplaints = filter === "all"
    ? complaints
    : complaints.filter((c) => c.status === filter);

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="container mx-auto px-4 py-8">
          <Card className="p-8">
            <div className="text-center space-y-4">
              <AlertCircle className="h-12 w-12 mx-auto text-red-500" />
              <p className="text-lg text-red-600">{error}</p>
              <Button onClick={fetchComplaints}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Thử lại
              </Button>
            </div>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold">Quản lý khiếu nại</h1>
          <Button
            variant="outline"
            onClick={fetchComplaints}
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>

        {/* Filter buttons */}
        <div className="flex gap-2 mb-6">
          {[
            { value: "all", label: "Tất cả" },
            { value: "OPEN", label: "Chưa xử lý" },
            { value: "IN_PROGRESS", label: "Đang xử lý" },
            { value: "RESOLVED", label: "Đã giải quyết" },
          ].map((f) => (
            <Button
              key={f.value}
              variant={filter === f.value ? "default" : "outline"}
              size="sm"
              onClick={() => setFilter(f.value)}
            >
              {f.label}
              <Badge variant="secondary" className="ml-2">
                {f.value === "all"
                  ? complaints.length
                  : complaints.filter((c) => c.status === f.value).length}
              </Badge>
            </Button>
          ))}
        </div>

        {loading ? (
          <div className="flex items-center justify-center p-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="ml-3 text-muted-foreground">Đang tải...</span>
          </div>
        ) : filteredComplaints.length === 0 ? (
          <Card className="p-8">
            <div className="text-center text-muted-foreground">
              <AlertTriangle className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Không có khiếu nại nào.</p>
            </div>
          </Card>
        ) : (
          <div className="space-y-4">
            {filteredComplaints.map((complaint) => (
              <Card key={complaint.complaintId} className={
                complaint.status === "OPEN"
                  ? "border-l-4 border-l-red-500"
                  : complaint.status === "RESOLVED"
                  ? "border-l-4 border-l-green-500"
                  : ""
              }>
                <CardContent className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <div className="flex items-center gap-3 mb-1">
                        <p className="font-semibold">{complaint.complainer.fullName}</p>
                        {getStatusBadge(complaint.status)}
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {complaint.stadium.stadiumName} · Đơn #{complaint.bookingId}
                      </p>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {formatDate(complaint.createdAt)}
                    </p>
                  </div>

                  {/* Complaint content */}
                  <div className="bg-red-50 dark:bg-red-950/20 p-3 rounded-lg mb-4">
                    <p className="text-sm">{complaint.content}</p>
                  </div>

                  {/* Contact info */}
                  <div className="flex gap-4 text-xs text-muted-foreground mb-4">
                    <span>📧 {complaint.complainer.email}</span>
                    <span>📞 {complaint.complainer.phoneNumber}</span>
                  </div>

                  {/* Owner response */}
                  {complaint.response ? (
                    <div className="bg-green-50 dark:bg-green-950/20 border-l-4 border-green-500 p-3 rounded-r-lg">
                      <p className="text-xs font-semibold text-green-700 mb-1">
                        Phản hồi của bạn:
                      </p>
                      <p className="text-sm">{complaint.response}</p>
                    </div>
                  ) : complaint.status !== "RESOLVED" ? (
                    <div>
                      {resolvingId === complaint.complaintId ? (
                        <div className="space-y-2">
                          <textarea
                            placeholder="Nhập phản hồi giải quyết khiếu nại..."
                            value={responseText}
                            onChange={(e) => setResponseText(e.target.value)}
                            className="w-full border rounded-lg px-3 py-2 text-sm min-h-[100px] resize-none"
                          />
                          <div className="flex gap-2 justify-end">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => {
                                setResolvingId(null);
                                setResponseText("");
                              }}
                            >
                              Hủy
                            </Button>
                            <Button
                              size="sm"
                              onClick={() => handleResolve(complaint.complaintId)}
                              disabled={processingId === complaint.complaintId}
                            >
                              {processingId === complaint.complaintId ? (
                                <Loader2 className="h-4 w-4 animate-spin mr-1" />
                              ) : (
                                <Send className="h-4 w-4 mr-1" />
                              )}
                              Giải quyết
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setResolvingId(complaint.complaintId)}
                        >
                          <Send className="h-4 w-4 mr-1" />
                          Phản hồi & Giải quyết
                        </Button>
                      )}
                    </div>
                  ) : null}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default ComplaintManagementPage;
