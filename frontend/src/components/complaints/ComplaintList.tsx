import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AlertCircle, MessageSquare, ChevronDown, ChevronUp, CheckCircle2 } from "lucide-react";
import { get, post } from "@/lib/api";
import { toast } from "sonner";

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
  bookingId?: string;
};

export function ComplaintList({ isOwner }: { isOwner: boolean }) {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [replyText, setReplyText] = useState("");
  const [resolveText, setResolveText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showDetails, setShowDetails] = useState(true);

  useEffect(() => {
    if (selectedComplaint) {
      setShowDetails(selectedComplaint.status !== "resolved");
    }
  }, [selectedComplaint]);

  const fetchComplaints = useCallback(async () => {
    try {
      const endpoint = isOwner ? "/owner/complaints" : "/complaints";
      const data = await get<Complaint[]>(endpoint);
      if (data && Array.isArray(data)) {
        setComplaints(data);
      } else {
        setComplaints([]);
      }
    } catch (error) {
      console.error("Failed to load complaints", error);
    }
  }, [isOwner]);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  const handleReply = async () => {
    if (!replyText.trim() || !selectedComplaint) return;
    try {
      setSubmitting(true);
      const endpoint = isOwner 
        ? `/owner/complaints/${selectedComplaint.complaintId}/reply` 
        : `/complaints/${selectedComplaint.complaintId}/reply`;
        
      const data = await post<Complaint>(endpoint, { message: replyText.trim() });
      
      const updated = complaints.map(c => c.complaintId === data.complaintId ? data : c);
      setComplaints(updated);
      setSelectedComplaint(data);
      setReplyText("");
      toast.success("Đã gửi phản hồi!");
    } catch (error: any) {
      toast.error(error.message || "Lỗi khi gửi phản hồi");
    } finally {
      setSubmitting(false);
    }
  };

  const handleResolve = async () => {
    if (!resolveText.trim() || !selectedComplaint || !isOwner) return;
    try {
      setSubmitting(true);
      const data = await post<Complaint>(`/owner/complaints/${selectedComplaint.complaintId}/resolve`, { 
        resolution: resolveText.trim() 
      });
      
      const updated = complaints.map(c => c.complaintId === data.complaintId ? data : c);
      setComplaints(updated);
      setSelectedComplaint(data);
      setResolveText("");
      toast.success("Đã đánh dấu giải quyết khiếu nại!");
    } catch (error: any) {
      toast.error(error.message || "Lỗi khi đóng khiếu nại");
    } finally {
      setSubmitting(false);
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
    <div className="space-y-4">
      {complaints.map((complaint) => (
        <Card
          key={complaint.complaintId}
          className="cursor-pointer hover:shadow-lg transition-shadow bg-white"
          onClick={() => setSelectedComplaint(complaint)}
        >
          <CardContent className="p-6">
            <div className="flex items-start justify-between mb-4">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <span className="font-mono text-sm">{complaint.id}</span>
                  {getStatusBadge(complaint.status)}
                  {getPriorityBadge(complaint.priority)}
                </div>
                <h3 className="mb-1 font-semibold text-lg">{complaint.subject}</h3>
                <p className="text-sm text-muted-foreground mb-2">
                  {isOwner ? "Sân bị khiếu nại:" : "Khiếu nại về:"} <strong>{complaint.against}</strong>
                </p>
                <p className="text-sm text-muted-foreground line-clamp-2">
                  {complaint.description}
                </p>
              </div>
              <div className="text-sm text-muted-foreground">
                {new Date(complaint.submittedDate).toLocaleDateString('vi-VN')}
              </div>
            </div>

            {complaint.responses && complaint.responses.length > 0 && (
              <div className="flex items-center gap-2 text-sm text-primary font-medium">
                <MessageSquare className="h-4 w-4" />
                <span>{complaint.responses.length} tin nhắn trao đổi</span>
              </div>
            )}
          </CardContent>
        </Card>
      ))}

      {complaints.length === 0 && (
        <Card>
          <CardContent className="p-12 text-center text-muted-foreground bg-white">
            <AlertCircle className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>{isOwner ? "Bạn chưa có khiếu nại nào từ khách hàng" : "Bạn chưa tạo khiếu nại nào"}</p>
          </CardContent>
        </Card>
      )}

      {/* Complaint Detail Dialog */}
      <Dialog
        open={!!selectedComplaint}
        onOpenChange={() => {
          setSelectedComplaint(null);
          setReplyText("");
          setResolveText("");
        }}
      >
        <DialogContent className="max-w-2xl h-[85vh] flex flex-col p-6 gap-4 overflow-hidden">
          <DialogHeader className="flex-shrink-0">
            <DialogTitle>Chi tiết khiếu nại</DialogTitle>
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
                      Sân liên quan: <strong className="text-foreground">{activeComplaint.against}</strong>
                    </p>
                    {activeComplaint.bookingId && (
                      <p>
                        Mã đặt sân: <strong className="font-mono text-foreground">#{activeComplaint.bookingId}</strong>
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
                    {activeComplaint.responses.map((response: ComplaintResponse, idx: number) => {
                      const isAdmin = response.from === "Admin";
                      const isMe = !isAdmin && (isOwner ? (response.from === "Chủ sân") : (response.from === "Khách hàng"));
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

              {/* Reply / Resolve Actions (Fixed at the Bottom) */}
              {activeComplaint.status !== "resolved" && (
                <div className="flex-shrink-0 pt-4 border-t space-y-4">
                  <div className="space-y-2">
                    <Label>Nhắn tin phản hồi</Label>
                    <div className="flex gap-2">
                      <Input
                        placeholder="Nhập nội dung phản hồi..."
                        value={replyText}
                        onChange={e => setReplyText(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleReply()}
                      />
                      <Button onClick={handleReply} disabled={!replyText.trim() || submitting}>Gửi</Button>
                    </div>
                  </div>

                  {isOwner && (
                    <div className="space-y-2 pt-4 border-t">
                      <Label className="text-green-700 font-semibold">Đóng & Giải quyết khiếu nại</Label>
                      <div className="flex gap-2">
                        <Input
                          placeholder="Nhập hướng giải quyết (VD: Đã hoàn tiền)..."
                          value={resolveText}
                          onChange={e => setResolveText(e.target.value)}
                        />
                        <Button variant="outline" className="bg-green-50 text-green-700 hover:bg-green-100 border-green-200" onClick={handleResolve} disabled={!resolveText.trim() || submitting}>
                          Giải quyết xong
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
    </div>
  );
}
