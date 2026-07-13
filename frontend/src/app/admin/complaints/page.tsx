'use client'

import { useState, useEffect, useCallback } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  AlertCircle,
  MessageSquare,
  Send,
  CheckCircle2,
  Clock,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { get, post } from "@/lib/api";
import { toast } from "sonner";
import { useComplaintWebSocket, type ComplaintChatEvent } from "@/hooks/useComplaintWebSocket";
import type { PageResponse } from "@/types/common";

type ResponseItem = {
  from: string;
  message: string;
  time: string;
};

type Complaint = {
  complaintId: number;
  subject: string;
  description: string;
  status: string;
  priority: string;
  submittedDate: string;
  resolvedDate?: string;
  resolution?: string;
  bookingId: number;
  bookingStatus: string;
  stadiumId: number;
  stadiumName: string;
  ownerName: string;
  ownerEmail: string;
  customerName: string;
  customerEmail: string;
  responses: ResponseItem[];
  escalationReason?: string;
};

function AdminComplaintsPage() {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [replyMessage, setReplyMessage] = useState("");
  const [resolutionText, setResolutionText] = useState("");
  const [showResolveDialog, setShowResolveDialog] = useState(false);
  const [filterStatus, setFilterStatus] = useState("all");
  const [filterPriority, setFilterPriority] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [stats, setStats] = useState({ totalCount: 0, openCount: 0, progressCount: 0, escalatedCount: 0, resolvedCount: 0 });
  const [showApproveDialog, setShowApproveDialog] = useState(false);
  const [showOverrideDialog, setShowOverrideDialog] = useState(false);
  const [overrideText, setOverrideText] = useState("");

  const fetchComplaints = useCallback(async () => {
    try {
      setIsLoading(true);
      const [data, statsData] = await Promise.all([
        get<PageResponse<Complaint>>(`/admin/complaints?page=${page}&size=20`),
        get<any>(`/admin/complaints/stats`)
      ]);
      const list = data.content;
      setStats(statsData);
      if (Array.isArray(list)) {
        setComplaints(list);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        setError(null);
        setSelectedComplaint(prev => {
          if (prev) {
            return list.find(c => c.complaintId === prev.complaintId) ?? null;
          }
          return null;
        });
      }
    } catch {
      setError("Không thể tải dữ liệu khiếu nại. Vui lòng kiểm tra kết nối và thử lại.");
    } finally {
      setIsLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  // Real-time: append new chat event from WebSocket without full re-fetch
  const handleWsEvent = useCallback((event: ComplaintChatEvent) => {
    setComplaints(prev => prev.map(c => {
      if (c.complaintId !== event.complaintId) return c;
      const alreadyExists = c.responses.some(
        r => r.from === event.from && r.message === event.message && r.time === event.time
      );
      if (alreadyExists) return c;
      return {
        ...c,
        status: event.newStatus || c.status,
        responses: [...c.responses, { from: event.from, message: event.message, time: event.time }],
      };
    }));
    setSelectedComplaint(prev => {
      if (!prev || prev.complaintId !== event.complaintId) return prev;
      const alreadyExists = prev.responses.some(
        r => r.from === event.from && r.message === event.message && r.time === event.time
      );
      if (alreadyExists) return prev;
      return {
        ...prev,
        status: event.newStatus || prev.status,
        responses: [...prev.responses, { from: event.from, message: event.message, time: event.time }],
      };
    });
  }, []);

  useComplaintWebSocket(selectedComplaint?.complaintId ?? null, handleWsEvent);

  const handleSendMessage = async () => {
    if (!replyMessage.trim() || !selectedComplaint) return;

    try {
      await post<unknown>(`/admin/complaints/${selectedComplaint.complaintId}/reply`, {
        message: replyMessage.trim()
      });
      setReplyMessage("");
      toast.success("Gửi phản hồi thành công!");
    } catch {
      toast.error("Gửi phản hồi thất bại. Vui lòng thử lại.");
    }
  };

  const handleResolveComplaint = async () => {
    if (!resolutionText.trim() || !selectedComplaint) return;

    try {
      await post<unknown>(`/admin/complaints/${selectedComplaint.complaintId}/resolve`, {
        resolution: resolutionText.trim()
      });
      setResolutionText("");
      setShowResolveDialog(false);
      toast.success("Giải quyết khiếu nại thành công!");
      fetchComplaints();
    } catch {
      toast.error("Giải quyết khiếu nại thất bại. Vui lòng thử lại.");
    }
  };

  const handleApproveResolution = async () => {
    if (!selectedComplaint) return;
    try {
      await post(`/admin/complaints/${selectedComplaint.complaintId}/approve`, {});
      setShowApproveDialog(false);
      toast.success("Đã chấp nhận giải pháp của chủ sân!");
      fetchComplaints();
    } catch {
      toast.error("Không thể chấp nhận giải pháp.");
    }
  };

  const handleOverrideResolution = async () => {
    if (!overrideText.trim() || !selectedComplaint) return;
    try {
      await post(`/admin/complaints/${selectedComplaint.complaintId}/override`, {
        resolution: overrideText.trim(),
      });
      setOverrideText("");
      setShowOverrideDialog(false);
      toast.success("Đã ghi đè giải pháp của chủ sân!");
      fetchComplaints();
    } catch {
      toast.error("Không thể ghi đè giải pháp.");
    }
  };

  const getPriorityConfig = (priority?: string) => {
    switch (priority?.toLowerCase()) {
      case "high":
        return { label: "Cao", color: "bg-red-100 text-red-700 border-red-200" };
      case "low":
        return { label: "Thấp", color: "bg-emerald-100 text-emerald-700 border-emerald-200" };
      default:
        return { label: "Trung bình", color: "bg-amber-100 text-amber-700 border-amber-200" };
    }
  };

  const getStatusConfig = (status: string) => {
    switch (status.toLowerCase()) {
      case "resolved":
        return { label: "Đã giải quyết", color: "bg-emerald-500 text-white" };
      case "in_progress":
        return { label: "Đang xử lý", color: "bg-blue-500 text-white" };
      case "escalated":
        return { label: "Đã chuyển Admin", color: "bg-purple-500 text-white" };
      case "awaiting_customer_response":
        return { label: "Chờ xem xét", color: "bg-orange-500 text-white" };
      case "customer_withdrawn":
        return { label: "Khách rút", color: "bg-slate-500 text-white" };
      default:
        return { label: "Mới nhận", color: "bg-amber-500 text-white" };
    }
  };

  const { totalCount, openCount, progressCount, escalatedCount, resolvedCount } = stats;

  const filteredComplaints = complaints.filter(c => {
    const matchesStatus = filterStatus === "all" || c.status.toLowerCase() === filterStatus.toLowerCase();
    const matchesPriority = filterPriority === "all" || c.priority.toLowerCase() === filterPriority.toLowerCase();
    const matchesSearch = !searchTerm ||
      (c.subject || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (c.description || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (c.customerName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (c.stadiumName || '').toLowerCase().includes(searchTerm.toLowerCase());
    return matchesStatus && matchesPriority && matchesSearch;
  });

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[400px]">
        <div className="text-center space-y-2">
          <div className="h-8 w-8 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-sm text-muted-foreground">Đang tải dữ liệu...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 flex flex-col items-center justify-center min-h-[400px] gap-4">
        <AlertCircle className="h-12 w-12 text-destructive" />
        <p className="text-sm text-destructive font-medium">{error}</p>
        <Button variant="outline" onClick={fetchComplaints}>Thử lại</Button>
      </div>
    );
  }

  return (
    <div className="p-8 flex flex-col gap-6 bg-muted/10 min-h-[calc(100vh-64px)]">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight text-foreground">Quản lý khiếu nại hệ thống</h1>
        <p className="text-sm text-muted-foreground mt-1">Giám sát và hòa giải khiếu nại giữa khách hàng và các chủ sân thể thao.</p>
      </div>

      {/* Quick Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <Card><CardContent className="p-5">
          <div className="text-xs text-muted-foreground font-medium">Tổng khiếu nại</div>
          <div className="text-2xl font-bold text-foreground mt-1">{totalCount}</div>
        </CardContent></Card>
        <Card><CardContent className="p-5">
          <div className="text-xs text-muted-foreground font-medium">Chưa phản hồi (Mới)</div>
          <div className="text-2xl font-bold text-amber-500 mt-1">{openCount}</div>
        </CardContent></Card>
        <Card><CardContent className="p-5">
          <div className="text-xs text-muted-foreground font-medium">Đang giải quyết</div>
          <div className="text-2xl font-bold text-blue-500 mt-1">{progressCount}</div>
        </CardContent></Card>
        <Card><CardContent className="p-5">
          <div className="text-xs text-muted-foreground font-medium">Cần Admin xử lý</div>
          <div className="text-2xl font-bold text-purple-500 mt-1">{escalatedCount}</div>
        </CardContent></Card>
        <Card><CardContent className="p-5">
          <div className="text-xs text-muted-foreground font-medium">Đã đóng</div>
          <div className="text-2xl font-bold text-emerald-500 mt-1">{resolvedCount}</div>
        </CardContent></Card>
      </div>

      {/* Filters Bar */}
      <div className="flex flex-col sm:flex-row gap-3 bg-card p-4 rounded-lg border shadow-sm">
        <div className="flex-1">
          <Input
            placeholder="Tìm theo chủ đề, nội dung, tên khách hàng, tên sân..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="flex gap-3">
          <Select value={filterStatus} onValueChange={setFilterStatus}>
            <SelectTrigger className="w-40"><SelectValue placeholder="Trạng thái" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Mọi trạng thái</SelectItem>
              <SelectItem value="open">Mới nhận</SelectItem>
              <SelectItem value="in_progress">Đang xử lý</SelectItem>
              <SelectItem value="escalated">Đã chuyển Admin</SelectItem>
              <SelectItem value="awaiting_customer_response">Chờ xem xét</SelectItem>
              <SelectItem value="resolved">Đã giải quyết</SelectItem>
              <SelectItem value="customer_withdrawn">Khách rút</SelectItem>
            </SelectContent>
          </Select>
          <Select value={filterPriority} onValueChange={setFilterPriority}>
            <SelectTrigger className="w-40"><SelectValue placeholder="Độ ưu tiên" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Mọi ưu tiên</SelectItem>
              <SelectItem value="high">Ưu tiên Cao</SelectItem>
              <SelectItem value="medium">Trung bình</SelectItem>
              <SelectItem value="low">Thấp</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Split View */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6 min-h-[550px]">
        {/* List side */}
        <div className="lg:col-span-5 flex flex-col gap-3 max-h-[700px] overflow-y-auto pr-1">
          {filteredComplaints.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center border border-dashed rounded-xl p-8 text-muted-foreground bg-card">
              <AlertCircle className="h-10 w-10 text-muted-foreground/40 mb-2" />
              <p className="text-sm">Không tìm thấy khiếu nại nào phù hợp</p>
            </div>
          ) : (
            filteredComplaints.map(c => {
              const priority = getPriorityConfig(c.priority);
              const status = getStatusConfig(c.status);
              const isSelected = selectedComplaint?.complaintId === c.complaintId;
              return (
                <Card
                  key={c.complaintId}
                  onClick={() => setSelectedComplaint(c)}
                  className={`cursor-pointer transition-all border shadow-sm ${
                    isSelected
                      ? "bg-primary/5 border-primary/40 shadow-primary/10"
                      : "bg-card hover:border-primary/20 hover:shadow-md"
                  }`}
                >
                  <CardContent className="p-4 flex flex-col gap-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-mono text-muted-foreground">ID: #{c.complaintId}</span>
                      <div className="flex gap-1.5">
                        <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full border ${priority.color}`}>
                          {priority.label}
                        </span>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${status.color}`}>
                          {status.label}
                        </span>
                      </div>
                    </div>
                    <div>
                      <h3 className="font-semibold text-sm text-foreground line-clamp-1">{c.subject}</h3>
                      <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{c.description}</p>
                    </div>
                    <div className="flex items-center justify-between border-t pt-3 text-[11px] text-muted-foreground">
                      <div>Khách: <span className="text-foreground font-medium">{c.customerName}</span></div>
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {c.submittedDate}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            })
          )}
          {totalPages > 1 && (
            <div className="sticky bottom-0 flex items-center justify-center gap-3 rounded-lg border bg-card p-3 shadow-sm">
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() => setPage(current => Math.max(0, current - 1))}
                disabled={page === 0 || isLoading}
                aria-label="Trang khiếu nại trước"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-xs font-medium text-muted-foreground">
                Trang {page + 1} / {totalPages}
              </span>
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() => setPage(current => Math.min(totalPages - 1, current + 1))}
                disabled={page >= totalPages - 1 || isLoading}
                aria-label="Trang khiếu nại sau"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </div>

        {/* Chat/Detail side */}
        <div className="lg:col-span-7 flex flex-col bg-card border rounded-xl overflow-hidden shadow-sm max-h-[700px]">
          {selectedComplaint ? (
            <div className="flex-1 flex flex-col h-full overflow-hidden">
              {/* Header */}
              <div className="p-5 border-b bg-card flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
                <div>
                  <div className="flex items-center gap-2 mb-1.5">
                    <span className="text-xs font-mono text-muted-foreground">Khiếu nại #{selectedComplaint.complaintId}</span>
                    <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full border ${getPriorityConfig(selectedComplaint.priority).color}`}>
                      {getPriorityConfig(selectedComplaint.priority).label}
                    </span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${getStatusConfig(selectedComplaint.status).color}`}>
                      {getStatusConfig(selectedComplaint.status).label}
                    </span>
                  </div>
                  <h2 className="text-base font-semibold text-foreground">{selectedComplaint.subject}</h2>
                </div>
                {selectedComplaint.status !== "resolved" && selectedComplaint.status !== "customer_withdrawn" && (
                  <div className="flex flex-wrap gap-2">
                    {["awaiting_customer_response", "escalated"].includes(selectedComplaint.status) && (
                      <>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setShowApproveDialog(true)}
                          className="font-medium"
                        >
                          Chấp nhận giải pháp Owner
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setShowOverrideDialog(true)}
                          className="font-medium"
                        >
                          Ghi đè giải pháp
                        </Button>
                      </>
                    )}
                    <Button
                      size="sm"
                      onClick={() => setShowResolveDialog(true)}
                      className="bg-primary hover:bg-primary/90 text-primary-foreground font-medium flex items-center gap-1.5"
                    >
                      <CheckCircle2 className="h-4 w-4" />
                      Đóng khiếu nại
                    </Button>
                  </div>
                )}
              </div>

              {/* Complaint Details Panel */}
              <div className="px-5 py-4 bg-muted/20 border-b text-xs grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <p className="text-muted-foreground font-medium">Người gửi khiếu nại</p>
                  <p className="font-semibold text-foreground mt-0.5">{selectedComplaint.customerName}</p>
                  <p className="text-[10px] text-muted-foreground font-mono">{selectedComplaint.customerEmail}</p>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium">Đối tượng bị khiếu nại</p>
                  <p className="font-semibold text-foreground mt-0.5">{selectedComplaint.stadiumName}</p>
                  <p className="text-[10px] text-muted-foreground font-mono">Chủ: {selectedComplaint.ownerName}</p>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium">Đơn đặt sân liên quan</p>
                  <p className="font-semibold text-foreground mt-0.5">Booking #{selectedComplaint.bookingId}</p>
                  <p className="text-[10px] text-primary font-bold">{selectedComplaint.bookingStatus}</p>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium">Thời gian tạo</p>
                  <p className="font-semibold text-foreground mt-0.5">{selectedComplaint.submittedDate}</p>
                </div>
              </div>

              {/* Message / Chat Thread */}
              <div className="flex-1 p-5 overflow-y-auto bg-muted/5 space-y-4">
                {/* Customer original complaint */}
                <div className="flex gap-3 items-start max-w-[85%]">
                  <Avatar className="h-8 w-8 flex-shrink-0">
                    <AvatarFallback className="text-xs bg-primary/20 text-primary font-bold">KH</AvatarFallback>
                  </Avatar>
                  <div className="bg-card border rounded-2xl rounded-tl-none p-3.5 shadow-sm text-sm">
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className="font-semibold text-xs text-primary">Khách hàng: {selectedComplaint.customerName}</span>
                      <span className="text-[10px] text-muted-foreground font-mono">{selectedComplaint.submittedDate}</span>
                    </div>
                    <p className="text-foreground leading-relaxed">{selectedComplaint.description}</p>
                  </div>
                </div>

                {selectedComplaint.escalationReason && (
                  <div className="flex justify-center my-2">
                    <div className="bg-purple-50 border border-purple-200 rounded-xl p-3 max-w-[85%] shadow-sm w-full">
                      <div className="flex items-center text-xs text-purple-700 font-bold gap-1 mb-1">
                        <AlertCircle className="h-3.5 w-3.5" /> Lý do chuyển Admin (từ hệ thống/khách hàng)
                      </div>
                      <p className="text-sm font-medium text-purple-800">{selectedComplaint.escalationReason}</p>
                    </div>
                  </div>
                )}

                {/* Chat replies */}
                {selectedComplaint.responses.map((resp, idx) => {
                  const isProposal = resp.message.startsWith("Đã đề xuất giải pháp:");
                  const isObjection = resp.message.startsWith("Khách hàng phản đối:");

                  if (isProposal) {
                    return (
                      <div key={idx} className="flex justify-center my-2 w-full">
                        <div className="bg-orange-50 border border-orange-200 rounded-lg p-3 text-sm max-w-[85%] shadow-sm w-full">
                          <div className="flex items-center text-orange-700 font-bold gap-1 mb-1">
                            <AlertCircle className="h-4 w-4" /> Đã đề xuất giải pháp
                          </div>
                          <p className="text-orange-800 whitespace-pre-wrap">{resp.message.replace("Đã đề xuất giải pháp: ", "")}</p>
                          <div className="text-[10px] text-orange-600/80 mt-1.5 font-mono text-right">{resp.time}</div>
                        </div>
                      </div>
                    );
                  }

                  if (isObjection) {
                    return (
                      <div key={idx} className="flex justify-center my-2 w-full">
                        <div className="bg-purple-50 border border-purple-200 rounded-lg p-3 text-sm max-w-[85%] shadow-sm w-full">
                          <div className="flex items-center text-purple-700 font-bold gap-1 mb-1">
                            <AlertCircle className="h-4 w-4" /> Khách hàng phản đối
                          </div>
                          <p className="text-purple-800 whitespace-pre-wrap">{resp.message.replace("Khách hàng phản đối: ", "")}</p>
                          <div className="text-[10px] text-purple-600/80 mt-1.5 font-mono text-right">{resp.time}</div>
                        </div>
                      </div>
                    );
                  }

                  const isAdminMsg = resp.from === "Admin";
                  const isOwnerMsg = resp.from === "Chủ sân";
                  const ownerLabel = `Chủ sân: ${selectedComplaint.ownerName ?? 'N/A'}`;
                  const customerLabel = `Khách hàng: ${selectedComplaint.customerName ?? 'N/A'}`;
                  const senderLabel = isAdminMsg ? "Quản trị viên (Bạn)" : isOwnerMsg ? ownerLabel : customerLabel;

                  return (
                    <div
                      key={idx}
                      className={`flex gap-3 items-start max-w-[85%] ${isAdminMsg ? "ml-auto justify-end" : ""}`}
                    >
                      {!isAdminMsg && (
                        <Avatar className="h-8 w-8 flex-shrink-0">
                          <AvatarFallback className={`text-[10px] font-bold ${
                            isOwnerMsg ? "bg-amber-100 text-amber-700" : "bg-primary/20 text-primary"
                          }`}>
                            {isOwnerMsg ? "CS" : "KH"}
                          </AvatarFallback>
                        </Avatar>
                      )}
                      <div className={`p-3.5 rounded-2xl shadow-sm text-sm border ${
                        isAdminMsg
                          ? "bg-primary/10 border-primary/20 rounded-tr-none"
                          : isOwnerMsg
                          ? "bg-amber-50 border-amber-200 rounded-tl-none"
                          : "bg-card border rounded-tl-none"
                      }`}>
                        <div className="flex items-center gap-2 mb-1.5">
                          <span className={`font-semibold text-xs ${
                            isAdminMsg ? "text-primary" : isOwnerMsg ? "text-amber-600" : "text-primary"
                          }`}>
                            {senderLabel}
                          </span>
                          <span className="text-[10px] text-muted-foreground font-mono">{resp.time}</span>
                        </div>
                        <p className="text-foreground leading-relaxed">{resp.message}</p>
                      </div>
                      {isAdminMsg && (
                        <Avatar className="h-8 w-8 flex-shrink-0">
                          <AvatarFallback className="text-[10px] bg-primary/20 text-primary font-bold">AD</AvatarFallback>
                        </Avatar>
                      )}
                    </div>
                  );
                })}

                {/* Resolution status */}
                {selectedComplaint.status === "resolved" && (
                  <div className="flex justify-center my-4">
                    <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3 text-center max-w-[85%] shadow-sm w-full">
                      <CheckCircle2 className="h-6 w-6 text-emerald-600 mx-auto mb-1.5" />
                      <h4 className="text-sm font-semibold text-emerald-700 mb-1">Khiếu nại này đã đóng & giải quyết</h4>
                      {selectedComplaint.resolution && (
                        <p className="text-xs text-emerald-600 italic mb-2 leading-relaxed">
                          "{selectedComplaint.resolution}"
                        </p>
                      )}
                      {selectedComplaint.resolvedDate && (
                        <p className="text-[10px] text-muted-foreground font-mono">Đóng vào: {selectedComplaint.resolvedDate}</p>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* Chat reply input */}
              {selectedComplaint.status !== "resolved" && selectedComplaint.status !== "customer_withdrawn" && (
                <div className="p-4 border-t bg-card flex gap-2 items-center">
                  <Input
                    placeholder="Nhập nội dung tin nhắn của Quản trị viên..."
                    value={replyMessage}
                    onChange={e => setReplyMessage(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === 'Enter' && replyMessage.trim()) handleSendMessage();
                    }}
                  />
                  <Button
                    size="icon"
                    onClick={handleSendMessage}
                    disabled={!replyMessage.trim()}
                    className="bg-primary hover:bg-primary/90 text-primary-foreground rounded-xl h-10 w-10 flex-shrink-0"
                  >
                    <Send className="h-4 w-4" />
                  </Button>
                </div>
              )}
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center p-8 text-muted-foreground">
              <MessageSquare className="h-12 w-12 text-muted-foreground/30 mb-3" />
              <h3 className="font-semibold text-base">Chưa chọn khiếu nại</h3>
              <p className="text-xs text-muted-foreground mt-1 max-w-[280px] text-center">
                Hãy bấm vào một khiếu nại ở danh sách bên trái để xem nội dung, lịch sử thảo luận và giải quyết tranh chấp.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Approve dialog */}
      <Dialog open={showApproveDialog} onOpenChange={setShowApproveDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Chấp nhận giải pháp của chủ sân</DialogTitle>
          </DialogHeader>
          <p className="text-xs text-muted-foreground">
            Xác nhận giải pháp hiện tại và đóng khiếu nại #{selectedComplaint?.complaintId}.
          </p>
          <div className="flex gap-2 justify-end pt-2">
            <Button variant="ghost" size="sm" onClick={() => setShowApproveDialog(false)}>Hủy</Button>
            <Button size="sm" onClick={handleApproveResolution}>Xác nhận</Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Override dialog */}
      <Dialog open={showOverrideDialog} onOpenChange={setShowOverrideDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Ghi đè giải pháp của chủ sân</DialogTitle>
          </DialogHeader>
          <Textarea
            placeholder="Nhập giải pháp mới của Admin..."
            value={overrideText}
            onChange={e => setOverrideText(e.target.value)}
            rows={4}
          />
          <div className="flex gap-2 justify-end pt-2">
            <Button variant="ghost" size="sm" onClick={() => setShowOverrideDialog(false)}>Hủy</Button>
            <Button size="sm" onClick={handleOverrideResolution} disabled={!overrideText.trim()}>
              Xác nhận ghi đè
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Resolve dialog */}
      <Dialog open={showResolveDialog} onOpenChange={setShowResolveDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <CheckCircle2 className="h-5 w-5 text-primary" />
              Đóng và giải quyết khiếu nại #{selectedComplaint?.complaintId}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-3">
            <p className="text-xs text-muted-foreground leading-relaxed">
              Bạn đang thực hiện đóng khiếu nại này với tư cách Quản trị viên. Nội dung giải pháp cuối cùng sẽ được ghi nhận và gửi thông báo đến cả Khách hàng và Chủ sân.
            </p>
            <div className="space-y-2">
              <label className="text-xs font-semibold text-foreground">Phương án giải quyết (Resolution)</label>
              <Textarea
                placeholder="Nhập chi tiết quyết định giải quyết của Admin (ví dụ: Hệ thống thực hiện hoàn tiền 100%, hoặc Yêu cầu chủ sân đền bù...)"
                value={resolutionText}
                onChange={e => setResolutionText(e.target.value)}
                rows={4}
              />
            </div>
          </div>
          <div className="flex gap-2 justify-end pt-2">
            <Button variant="ghost" size="sm" onClick={() => setShowResolveDialog(false)}>Hủy bỏ</Button>
            <Button
              size="sm"
              onClick={handleResolveComplaint}
              disabled={!resolutionText.trim()}
              className="bg-primary hover:bg-primary/90 text-primary-foreground font-medium"
            >
              Xác nhận Đóng khiếu nại
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default AdminComplaintsPage;
