'use client'

import { useState, useEffect, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AlertCircle, Plus, MessageSquare, XCircle, SlidersHorizontal, ChevronDown, ChevronUp, CheckCircle2 } from "lucide-react";
import { get, post } from "@/lib/api";
import { toast } from "sonner";
import { fetchMyBookings, type BookingHistoryItem } from "@/lib/bookings-api";
import { useComplaintWebSocket, type ComplaintChatEvent } from "@/hooks/useComplaintWebSocket";
import { useSearchParams } from "next/navigation";

type ComplaintResponse = {
  from: string;
  message: string;
  time: string;
};

type Complaint = {
  id: string;
  complaintId: number;
  subject: string;
  against: string;
  description: string;
  status: string;
  priority?: string;
  submittedDate: string;
  responses: ComplaintResponse[];
  resolvedDate?: string;
  resolution?: string;
  bookingId?: number;
  customerResponseDeadline?: string;
  escalatedAt?: string;
  escalationReason?: string;
};


function ComplaintsPage() {
  const searchParams = useSearchParams();
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [eligibleBookings, setEligibleBookings] = useState<BookingHistoryItem[]>([]);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);

  // Form states
  const [bookingId, setBookingId] = useState("");
  const [selectedBooking, setSelectedBooking] = useState<BookingHistoryItem | null>(null);
  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");

  // Filter & sort states
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [sortOrder, setSortOrder] = useState<string>("newest");

  // Reply states
  const [replyText, setReplyText] = useState("");
  const [submittingReply, setSubmittingReply] = useState(false);
  const [showDetails, setShowDetails] = useState(true);

  useEffect(() => {
    if (selectedComplaint) {
      setShowDetails(selectedComplaint.status !== "resolved");
    }
  }, [selectedComplaint]);

  // Close complaint states
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const [closingComplaint, setClosingComplaint] = useState(false);
  const [escalating, setEscalating] = useState(false);
  const [objecting, setObjecting] = useState(false);
  const [objectionText, setObjectionText] = useState("");
  const [showObjectDialog, setShowObjectDialog] = useState(false);

  const filteredComplaints = complaints
    .filter(c => statusFilter === "all" || c.status === statusFilter)
    .sort((a, b) => {
      const diff = new Date(b.submittedDate).getTime() - new Date(a.submittedDate).getTime();
      return sortOrder === "newest" ? diff : -diff;
    });

  const fetchComplaints = useCallback(async () => {
    try {
      const data = await get<{ content: Complaint[] }>("/complaints");
      const list = data.content;
      if (Array.isArray(list)) {
        setComplaints(list);
        setSelectedComplaint(prev =>
          prev ? (list.find(c => c.complaintId === prev.complaintId) ?? prev) : null
        );
      }
    } catch {
      toast.error("Không thể tải danh sách khiếu nại. Vui lòng thử lại.");
    }
  }, []);

  const fetchBookings = useCallback(async () => {
    try {
      const [completedRes, cancelledRes] = await Promise.all([
        fetchMyBookings(0, 100, "completed"),
        fetchMyBookings(0, 100, "cancelled"),
      ]);
      const bookings = [
        ...(completedRes?.bookings ?? []),
        ...(cancelledRes?.bookings ?? []),
      ];
      setEligibleBookings(bookings);
    } catch (error) {
      console.warn("Failed to fetch eligible bookings", error);
    }
  }, []);

  useEffect(() => {
    fetchComplaints();
    fetchBookings();
  }, [fetchComplaints, fetchBookings]);

  useEffect(() => {
    const complaintIdParam = searchParams.get("complaintId");
    if (!complaintIdParam || complaints.length === 0) return;
    const target = complaints.find(c => String(c.complaintId) === complaintIdParam);
    if (target) {
      setSelectedComplaint(target);
    }
  }, [searchParams, complaints]);

  const handleWsEvent = useCallback((event: ComplaintChatEvent) => {
    const appendMessage = (c: Complaint): Complaint => {
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
    };
    setComplaints(prev => prev.map(appendMessage));
    setSelectedComplaint(prev => (prev ? appendMessage(prev) : null));
  }, []);

  useComplaintWebSocket(selectedComplaint?.complaintId ?? null, handleWsEvent);

  const handleCreateComplaint = async () => {
    if (!subject.trim() || !description.trim() || !bookingId) return;

    try {
      await post<unknown>("/complaints", {
        bookingId: Number(bookingId),
        subject: subject.trim(),
        description: description.trim()
      });
      toast.success("Gửi khiếu nại thành công!");
      fetchComplaints();

      // Reset Form
      setSubject("");
      setDescription("");
      setBookingId("");
      setSelectedBooking(null);
      setShowCreateDialog(false);
    } catch (error: any) {
      toast.error(error.message || "Gửi khiếu nại thất bại.");
    }
  };

  const handleCloseComplaint = async () => {
    if (!selectedComplaint) return;
    setClosingComplaint(true);
    try {
      const data = await post<Complaint>(`/complaints/${selectedComplaint.complaintId}/close`, {});
      const updated = complaints.map(c => c.complaintId === data.complaintId ? data : c);
      setComplaints(updated);
      setSelectedComplaint(data);
      setShowCloseConfirm(false);
      toast.success("Đã đóng khiếu nại thành công!");
    } catch (error: any) {
      toast.error(error.message || "Lỗi khi đóng khiếu nại");
    } finally {
      setClosingComplaint(false);
    }
  };

  const handleSendReply = async () => {
    if (!replyText.trim() || !selectedComplaint) return;
    setSubmittingReply(true);
    try {
      const endpoint = `/complaints/${selectedComplaint.complaintId}/reply`;
      const data = await post<Complaint>(endpoint, { message: replyText.trim() });

      const updated = complaints.map(c => c.complaintId === data.complaintId ? data : c);
      setComplaints(updated);
      setSelectedComplaint(data);
      setReplyText("");
      toast.success("Đã gửi phản hồi!");
    } catch (error: any) {
      toast.error(error.message || "Lỗi khi gửi phản hồi");
    } finally {
      setSubmittingReply(false);
    }
  };

  const handleEscalateComplaint = async () => {
    if (!selectedComplaint) return;
    setEscalating(true);
    try {
      await post(`/complaints/${selectedComplaint.complaintId}/escalate`, {
        reason: "Không hài lòng với cách xử lý của chủ sân",
      });
      await fetchComplaints();
      toast.success("Đã chuyển khiếu nại lên Admin xử lý!");
    } catch (error: any) {
      toast.error(error.message || "Không thể escalate khiếu nại");
    } finally {
      setEscalating(false);
    }
  };

  const handleObjectToResolution = async () => {
    if (!selectedComplaint || !objectionText.trim()) return;
    setObjecting(true);
    try {
      await post(`/complaints/${selectedComplaint.complaintId}/object`, {
        reason: objectionText.trim(),
      });
      setShowObjectDialog(false);
      setObjectionText("");
      await fetchComplaints();
      toast.success("Đã gửi phản đối. Khiếu nại được chuyển lên Admin.");
    } catch (error: any) {
      toast.error(error.message || "Không thể gửi phản đối");
    } finally {
      setObjecting(false);
    }
  };

  const getStatusBadge = (status: string) => {
    const s = (status || "").toLowerCase();
    const config = {
      open: { label: "Mới", className: "bg-yellow-50 text-yellow-700 border-yellow-200" },
      in_progress: { label: "Đang xử lý", className: "bg-blue-50 text-blue-700 border-blue-200" },
      resolved: { label: "Đã giải quyết", className: "bg-green-50 text-green-700 border-green-200" },
      escalated: { label: "Đã chuyển Admin", className: "bg-purple-50 text-purple-700 border-purple-200" },
      pending_admin_review: { label: "Chờ phản hồi", className: "bg-orange-50 text-orange-700 border-orange-200" },
      customer_withdrawn: { label: "Đã rút", className: "bg-slate-50 text-slate-700 border-slate-200" },
    };
    const item = config[s as keyof typeof config] || { label: status, className: "bg-gray-50 text-gray-700 border-gray-200" };
    return <Badge variant="outline" className={item.className}>{item.label}</Badge>;
  };

  const getPriorityBadge = (priority?: string) => {
    const p = (priority || "").toLowerCase();
    const config = {
      low: { label: "Thấp", className: "bg-gray-100 text-gray-700 border-gray-200" },
      medium: { label: "Trung bình", className: "bg-orange-100 text-orange-700 border-orange-200" },
      high: { label: "Cao", className: "bg-red-100 text-red-700 border-red-200" },
    };
    const item = config[p as keyof typeof config] || { label: "Trung bình", className: "bg-orange-100 text-orange-700 border-orange-200" };
    return <Badge variant="outline" className={item.className}>{item.label}</Badge>;
  };

  const activeComplaint = selectedComplaint
    ? complaints.find(c => c.complaintId === selectedComplaint.complaintId) || selectedComplaint
    : null;

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-center justify-between mb-8">
            <h1 className="text-3xl font-bold bg-gradient-to-r from-primary to-orange-500 bg-clip-text text-transparent">Khiếu nại của tôi</h1>
            <Button onClick={() => setShowCreateDialog(true)} className="bg-primary hover:bg-primary/90 text-white shadow-md transition-all">
              <Plus className="mr-2 h-5 w-5" />
              Tạo khiếu nại
            </Button>
          </div>

          {/* Filter & Sort Bar */}
          <div className="flex items-center gap-3 mb-4 flex-wrap">
            <SlidersHorizontal className="h-4 w-4 text-muted-foreground shrink-0" />
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-44">
                <SelectValue placeholder="Lọc theo trạng thái" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả trạng thái</SelectItem>
                <SelectItem value="open">Mới</SelectItem>
                <SelectItem value="in_progress">Đang xử lý</SelectItem>
                <SelectItem value="resolved">Đã giải quyết</SelectItem>
                <SelectItem value="escalated">Đã chuyển Admin</SelectItem>
                <SelectItem value="pending_admin_review">Chờ phản hồi</SelectItem>
                <SelectItem value="customer_withdrawn">Đã rút</SelectItem>
              </SelectContent>
            </Select>
            <Select value={sortOrder} onValueChange={setSortOrder}>
              <SelectTrigger className="w-44">
                <SelectValue placeholder="Sắp xếp" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="newest">Mới nhất trước</SelectItem>
                <SelectItem value="oldest">Cũ nhất trước</SelectItem>
              </SelectContent>
            </Select>
            {(statusFilter !== "all" || sortOrder !== "newest") && (
              <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground"
                onClick={() => { setStatusFilter("all"); setSortOrder("newest"); }}
              >
                Xóa bộ lọc
              </Button>
            )}
          </div>

          <div className="space-y-4">
            {filteredComplaints.map((complaint) => (
              <Card
                key={complaint.complaintId}
                className="cursor-pointer hover:shadow-lg transition-all border hover:border-primary/30 duration-200"
                onClick={() => setSelectedComplaint(complaint)}
              >
                <CardContent className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="font-mono text-sm text-muted-foreground">{complaint.id}</span>
                        {getStatusBadge(complaint.status)}
                        {getPriorityBadge(complaint.priority)}
                      </div>
                      <h3 className="mb-1 font-semibold text-lg hover:text-primary transition-colors">{complaint.subject}</h3>
                      <p className="text-sm text-muted-foreground mb-2">
                        Khiếu nại về: <strong className="text-foreground">{complaint.against}</strong>
                      </p>
                      <p className="text-sm text-muted-foreground line-clamp-2 bg-muted/20 p-2 rounded">
                        {complaint.description}
                      </p>
                    </div>
                    <div className="text-sm text-muted-foreground pl-4">
                      {new Date(complaint.submittedDate).toLocaleDateString('vi-VN')}
                    </div>
                  </div>

                  {complaint.responses && complaint.responses.length > 0 && (
                    <div className="flex items-center gap-2 text-sm text-primary font-medium">
                      <MessageSquare className="h-4 w-4" />
                      <span>{complaint.responses.length} phản hồi từ chủ sân/admin</span>
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}

            {filteredComplaints.length === 0 && complaints.length > 0 && (
              <Card className="border-dashed">
                <CardContent className="p-12 text-center text-muted-foreground">
                  <AlertCircle className="h-12 w-12 mx-auto mb-3 opacity-30 text-primary" />
                  <p className="font-medium text-lg mb-1">Không có khiếu nại nào phù hợp</p>
                  <p className="text-sm opacity-80">Thử thay đổi bộ lọc để xem kết quả khác.</p>
                </CardContent>
              </Card>
            )}

            {complaints.length === 0 && (
              <Card className="border-dashed">
                <CardContent className="p-12 text-center text-muted-foreground">
                  <AlertCircle className="h-12 w-12 mx-auto mb-3 opacity-30 text-primary" />
                  <p className="font-medium text-lg mb-1">Bạn chưa có khiếu nại nào</p>
                  <p className="text-sm opacity-80">Nếu bạn gặp bất kỳ sự cố nào với các đơn đặt sân đã hoàn thành, hãy tạo khiếu nại để được hỗ trợ.</p>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>

      {/* Create Complaint Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold bg-gradient-to-r from-primary to-orange-500 bg-clip-text text-transparent">Tạo khiếu nại mới</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 pt-2">
            <div className="space-y-2">
              <Label htmlFor="bookingSelect">Chọn đơn đặt sân đã hoàn thành hoặc bị hủy *</Label>
              <Select
                value={bookingId}
                onValueChange={(val) => {
                  setBookingId(val);
                  const b = eligibleBookings.find(x => String(x.id) === val);
                  setSelectedBooking(b || null);
                }}
              >
                <SelectTrigger id="bookingSelect">
                  <SelectValue placeholder="Chọn đơn đặt sân của bạn" />
                </SelectTrigger>
                <SelectContent>
                  {eligibleBookings.map((b) => (
                    <SelectItem key={b.id} value={String(b.id)}>
                      {`Đơn #${b.id} - ${b.venue} (${b.date} - ${b.time})`}
                    </SelectItem>
                  ))}
                  {eligibleBookings.length === 0 && (
                    <SelectItem value="none" disabled>Không có đơn đặt sân phù hợp</SelectItem>
                  )}
                </SelectContent>
              </Select>
            </div>

            {selectedBooking && (
              <div className="bg-muted/40 p-4 rounded-lg border border-dashed text-sm space-y-1">
                <p><strong>Sân bóng:</strong> {selectedBooking.venue}</p>
                <p><strong>Thể loại:</strong> {selectedBooking.sportType}</p>
                <p><strong>Thời gian chơi:</strong> {selectedBooking.date} ({selectedBooking.time})</p>
                <p><strong>Số tiền:</strong> {selectedBooking.price?.toLocaleString('vi-VN')}đ</p>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="subject">Tiêu đề khiếu nại *</Label>
              <Input
                id="subject"
                placeholder="Vấn đề bạn gặp phải (ví dụ: Sân không đúng mô tả, sai thông tin...)"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Mô tả chi tiết *</Label>
              <Textarea
                id="description"
                placeholder="Mô tả chi tiết vấn đề bạn gặp phải, bằng chứng (nếu có) và hướng giải quyết bạn mong muốn từ chủ sân..."
                rows={6}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <div className="flex gap-3 pt-4 border-t">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => setShowCreateDialog(false)}
              >
                Hủy
              </Button>
              <Button
                className="flex-1 bg-primary hover:bg-primary/95 text-white"
                disabled={!subject.trim() || !description.trim() || !bookingId}
                onClick={handleCreateComplaint}
              >
                Gửi khiếu nại
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Complaint Detail Dialog */}
      <Dialog
        open={!!selectedComplaint}
        onOpenChange={() => {
          setSelectedComplaint(null);
          setReplyText("");
          setShowCloseConfirm(false);
        }}
      >
        <DialogContent className="max-w-2xl h-[85vh] flex flex-col p-6 gap-4 overflow-hidden">
          <DialogHeader className="flex-shrink-0">
            <DialogTitle className="text-xl font-bold">Chi tiết khiếu nại</DialogTitle>
          </DialogHeader>

          {activeComplaint && (
            <div className="flex-1 flex flex-col overflow-hidden min-h-0 gap-4">
              {/* Static Collapsible Details Area */}
              <div className="flex-shrink-0 border rounded-lg p-3 bg-muted/20">
                <div 
                  className="flex items-center justify-between cursor-pointer select-none" 
                  onClick={() => setShowDetails(!showDetails)}
                >
                  <div className="flex items-center gap-2 overflow-hidden">
                    <span className="font-mono text-xs text-muted-foreground shrink-0">{activeComplaint.id}</span>
                    <span className="font-bold text-sm text-foreground truncate">{activeComplaint.subject}</span>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {getStatusBadge(activeComplaint.status)}
                    <Button variant="ghost" size="sm" className="h-7 w-7 p-0 hover:bg-muted">
                      {showDetails ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </Button>
                  </div>
                </div>
                {showDetails && (
                  <div className="mt-3 pt-3 border-t space-y-2 text-xs text-muted-foreground animate-in fade-in duration-200">
                    <div className="flex justify-between items-center">
                      <span>Mức độ ưu tiên: {getPriorityBadge(activeComplaint.priority)}</span>
                      <span>Ngày gửi: {new Date(activeComplaint.submittedDate).toLocaleDateString('vi-VN')}</span>
                    </div>
                    <p>
                      Khiếu nại về: <strong className="text-foreground">{activeComplaint.against}</strong>
                    </p>
                    {activeComplaint.bookingId && (
                      <p>
                        Mã đặt sân liên quan: <strong className="font-mono text-foreground">#{activeComplaint.bookingId}</strong>
                      </p>
                    )}
                    <div className="bg-muted/40 p-2.5 rounded border text-foreground/80 leading-relaxed">
                      {activeComplaint.description}
                    </div>
                  </div>
                )}
              </div>

              {/* Scrollable Hộp Thư Trao Đổi */}
              {activeComplaint.responses && activeComplaint.responses.length > 0 && (
                <div className="flex-1 flex flex-col min-h-0 gap-2">
                  <h4 className="font-bold text-xs text-muted-foreground uppercase flex-shrink-0">Hộp thư trao đổi:</h4>
                  <div className="flex-1 overflow-y-auto pr-1 space-y-3 min-h-0">
                    {activeComplaint.responses.map((response, idx: number) => {
                      const isProposal = response.message.startsWith("Đã đề xuất giải pháp:");
                      const isObjection = response.message.startsWith("Khách hàng phản đối:");

                      if (isProposal) {
                        return (
                          <div key={idx} className="flex justify-center my-2 w-full">
                            <div className="bg-orange-50 border border-orange-200 rounded-lg p-3 text-sm max-w-[85%] shadow-sm w-full">
                              <div className="flex items-center text-orange-700 font-bold gap-1 mb-1">
                                <AlertCircle className="h-4 w-4" /> Chủ sân đề xuất giải pháp
                              </div>
                              <p className="text-orange-800 whitespace-pre-wrap">{response.message.replace("Đã đề xuất giải pháp: ", "")}</p>
                              <div className="text-[10px] text-orange-600/80 mt-1.5 font-mono text-right">{response.time}</div>
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
                              <p className="text-purple-800 whitespace-pre-wrap">{response.message.replace("Khách hàng phản đối: ", "")}</p>
                              <div className="text-[10px] text-purple-600/80 mt-1.5 font-mono text-right">{response.time}</div>
                            </div>
                          </div>
                        );
                      }

                      const isAdmin = response.from === "Admin";
                      const isMe = !isAdmin && response.from === "Khách hàng";
                      return (
                        <div
                          key={idx}
                          className={`flex flex-col max-w-[85%] rounded-lg p-3 ${
                            isMe
                              ? "bg-primary text-primary-foreground ml-auto shadow-sm"
                              : isAdmin
                              ? "bg-indigo-50 border-indigo-200 text-indigo-950 mr-auto border dark:bg-indigo-950/20 dark:border-indigo-900 dark:text-indigo-200"
                              : "bg-muted text-foreground mr-auto border"
                          }`}
                        >
                          <strong className="text-[10px] opacity-85 mb-1">{response.from}</strong>
                          <p className="text-sm leading-relaxed">{response.message}</p>
                          <span className="text-[9px] opacity-75 mt-1 self-end">{response.time}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Resolution (Static compact banner if resolved) */}
              {activeComplaint.status === "resolved" && activeComplaint.resolution && (
                <div className="flex-shrink-0 bg-green-50 border border-green-200 dark:bg-green-950/20 dark:border-green-900/50 rounded-lg p-3 flex gap-2.5 items-start max-w-[85%] mr-auto">
                  <CheckCircle2 className="h-5 w-5 text-green-700 dark:text-green-500 shrink-0 mt-0.5" />
                  <div className="space-y-1">
                    <p className="text-xs font-bold text-green-800 dark:text-green-400">Đã giải quyết thành công</p>
                    <p className="text-xs text-green-700 dark:text-green-300 leading-relaxed font-semibold">{activeComplaint.resolution}</p>
                    <p className="text-[10px] text-green-600 dark:text-green-500">
                      Ngày giải quyết: {activeComplaint.resolvedDate ? new Date(activeComplaint.resolvedDate).toLocaleDateString('vi-VN') : ''}
                    </p>
                  </div>
                </div>
              )}

              {/* Pending admin review banner */}
              {activeComplaint.status === "pending_admin_review" && (
                <div className="flex-shrink-0 bg-orange-50 border border-orange-200 rounded-lg p-3 space-y-2">
                  <p className="text-xs font-bold text-orange-800">Chủ sân đã đề xuất giải pháp</p>
                  <p className="text-xs text-orange-700">
                    Bạn có 48 giờ để phản hồi nếu không đồng ý.
                    {activeComplaint.customerResponseDeadline && (
                      <> Hạn: {new Date(activeComplaint.customerResponseDeadline).toLocaleString('vi-VN')}</>
                    )}
                  </p>
                  <div className="flex gap-2">
                    <Button size="sm" variant="outline" onClick={() => setShowObjectDialog(true)}>
                      Phản đối giải pháp
                    </Button>
                  </div>
                </div>
              )}

              {/* Reply Box (Fixed at the Bottom) */}
              {!["resolved", "customer_withdrawn"].includes(activeComplaint.status) && (
                <div className="flex-shrink-0 pt-4 border-t space-y-2">
                  <Label htmlFor="replyInput" className="font-semibold text-sm">Gửi phản hồi của bạn</Label>
                  <div className="flex gap-2">
                    <Input
                      id="replyInput"
                      placeholder="Nhập nội dung phản hồi..."
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      disabled={submittingReply}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" && replyText.trim() && !submittingReply) {
                          handleSendReply();
                        }
                      }}
                    />
                    <Button
                      onClick={handleSendReply}
                      disabled={!replyText.trim() || submittingReply}
                    >
                      Gửi
                    </Button>
                  </div>

                  {!showCloseConfirm ? (
                    <div className="flex flex-col gap-2">
                      {["open", "in_progress"].includes(activeComplaint.status) && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="w-full text-purple-700 hover:text-purple-800 hover:border-purple-200"
                          onClick={handleEscalateComplaint}
                          disabled={escalating}
                        >
                          {escalating ? "Đang chuyển..." : "Chuyển lên Admin"}
                        </Button>
                      )}
                      <Button
                        variant="outline"
                        size="sm"
                        className="w-full text-muted-foreground hover:text-red-600 hover:border-red-200"
                        onClick={() => setShowCloseConfirm(true)}
                      >
                        <XCircle className="h-4 w-4 mr-2" />
                        Rút khiếu nại
                      </Button>
                    </div>
                  ) : (
                    <div className="bg-orange-50 border border-orange-200 dark:bg-orange-950/20 dark:border-orange-900/50 rounded-lg p-3 space-y-2">
                      <p className="text-sm font-medium text-orange-800 dark:text-orange-300">Xác nhận đóng khiếu nại?</p>
                      <p className="text-xs text-orange-600 dark:text-orange-400">Khiếu nại sẽ được đánh dấu là đã giải quyết và không thể phản hồi thêm.</p>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          className="flex-1"
                          onClick={() => setShowCloseConfirm(false)}
                          disabled={closingComplaint}
                        >
                          Hủy
                        </Button>
                        <Button
                          size="sm"
                          className="flex-1 bg-red-600 hover:bg-red-700 text-white"
                          onClick={handleCloseComplaint}
                          disabled={closingComplaint}
                        >
                          {closingComplaint ? "Đang xử lý..." : "Xác nhận đóng"}
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Object to resolution dialog */}
      <Dialog open={showObjectDialog} onOpenChange={setShowObjectDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Phản đối giải pháp của chủ sân</DialogTitle>
          </DialogHeader>
          <Textarea
            placeholder="Mô tả lý do bạn không đồng ý..."
            value={objectionText}
            onChange={(e) => setObjectionText(e.target.value)}
            rows={4}
          />
          <div className="flex gap-2 justify-end">
            <Button variant="ghost" onClick={() => setShowObjectDialog(false)}>Hủy</Button>
            <Button
              onClick={handleObjectToResolution}
              disabled={!objectionText.trim() || objecting}
            >
              {objecting ? "Đang gửi..." : "Gửi phản đối"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <Footer />
    </div>
  );
}

export default ComplaintsPage;
