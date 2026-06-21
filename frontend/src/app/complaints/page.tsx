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
import { AlertCircle, Plus, MessageSquare, User, AlertTriangle, ChevronDown, ChevronUp, CheckCircle2 } from "lucide-react";
import { get, post } from "@/lib/api";
import { toast } from "sonner";
import { fetchMyBookings } from "@/lib/bookings-api";

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
};

const DEFAULT_COMPLAINTS: Complaint[] = [
  {
    id: "CP001",
    complaintId: 1,
    subject: "Sân không đúng mô tả",
    against: "Sân bóng Thành Công",
    description: "Sân thực tế không giống hình ảnh trên web. Cỏ nhân tạo cũ, bề mặt không bằng phẳng.",
    status: "open",
    priority: "medium",
    submittedDate: "2026-05-22",
    responses: [],
  },
  {
    id: "CP002",
    complaintId: 2,
    subject: "Chủ sân không phản hồi",
    against: "Sân bóng Thành Công",
    description: "Đã liên hệ nhiều lần nhưng chủ sân không phản hồi về việc hoàn tiền do hủy sân.",
    status: "in_progress",
    priority: "high",
    submittedDate: "2026-05-20",
    responses: [
      {
        from: "Admin",
        message: "Chúng tôi đang xem xét khiếu nại của bạn. Sẽ phản hồi trong 24-48h.",
        time: "2026-05-21 10:00",
      },
    ],
  },
];

function ComplaintsPage() {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [completedBookings, setCompletedBookings] = useState<any[]>([]);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);

  // Form states
  const [bookingId, setBookingId] = useState("");
  const [selectedBooking, setSelectedBooking] = useState<any>(null);
  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");

  // Reply states
  const [replyText, setReplyText] = useState("");
  const [submittingReply, setSubmittingReply] = useState(false);
  const [showDetails, setShowDetails] = useState(true);

  useEffect(() => {
    if (selectedComplaint) {
      setShowDetails(selectedComplaint.status !== "resolved");
    }
  }, [selectedComplaint]);

  const fetchComplaints = useCallback(async () => {
    try {
      const data = await get<Complaint[]>("/complaints");
      if (data && Array.isArray(data)) {
        setComplaints(data);
        localStorage.setItem('sport_venue_complaints', JSON.stringify(data));
      } else {
        throw new Error("Không có dữ liệu khiếu nại");
      }
    } catch (error) {
      console.warn("Backend offline or error, loading local fallback:", error);
      const stored = localStorage.getItem('sport_venue_complaints');
      if (stored) {
        setComplaints(JSON.parse(stored));
      } else {
        localStorage.setItem('sport_venue_complaints', JSON.stringify(DEFAULT_COMPLAINTS));
        setComplaints(DEFAULT_COMPLAINTS);
      }
    }
  }, []);

  const fetchBookings = useCallback(async () => {
    try {
      const res = await fetchMyBookings(0, 100, "completed");
      if (res && res.bookings) {
        setCompletedBookings(res.bookings);
      }
    } catch (error) {
      console.warn("Failed to fetch completed bookings", error);
    }
  }, []);

  useEffect(() => {
    fetchComplaints();
    fetchBookings();
  }, [fetchComplaints, fetchBookings]);

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

  const getStatusBadge = (status: string) => {
    const s = (status || "").toLowerCase();
    const config = {
      open: { label: "Mới", className: "bg-yellow-50 text-yellow-700 border-yellow-200" },
      in_progress: { label: "Đang xử lý", className: "bg-blue-50 text-blue-700 border-blue-200" },
      resolved: { label: "Đã giải quyết", className: "bg-green-50 text-green-700 border-green-200" },
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

          <div className="space-y-4">
            {complaints.map((complaint) => (
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
              <Label htmlFor="bookingSelect">Chọn đơn đặt sân đã hoàn thành *</Label>
              <Select 
                value={bookingId} 
                onValueChange={(val) => {
                  setBookingId(val);
                  const b = completedBookings.find(x => String(x.id) === val);
                  setSelectedBooking(b);
                }}
              >
                <SelectTrigger id="bookingSelect">
                  <SelectValue placeholder="Chọn đơn đặt sân của bạn" />
                </SelectTrigger>
                <SelectContent>
                  {completedBookings.map((b) => (
                    <SelectItem key={b.id} value={String(b.id)}>
                      {`Đơn #${b.id} - ${b.venue} (${b.date} - ${b.time})`}
                    </SelectItem>
                  ))}
                  {completedBookings.length === 0 && (
                    <SelectItem value="none" disabled>Không có đơn đặt sân nào đã hoàn thành</SelectItem>
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
                <div className="flex-shrink-0 bg-green-50 border border-green-200 dark:bg-green-950/20 dark:border-green-900/50 rounded-lg p-3 flex gap-2.5 items-start">
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

              {/* Reply Box (Fixed at the Bottom) */}
              {activeComplaint.status !== "resolved" && (
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
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>

      <Footer />
    </div>
  );
}

export default ComplaintsPage;
