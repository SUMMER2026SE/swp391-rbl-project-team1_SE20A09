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
import { AlertCircle, MessageSquare } from "lucide-react";
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
      open: { label: "Mới", className: "bg-yellow-100 text-yellow-700" },
      in_progress: { label: "Đang xử lý", className: "bg-blue-100 text-blue-700" },
      resolved: { label: "Đã giải quyết", className: "bg-green-100 text-green-700" },
    };
    const item = config[s as keyof typeof config] || { label: status, className: "bg-gray-100 text-gray-700" };
    return <Badge className={item.className}>{item.label}</Badge>;
  };

  const activeComplaint = selectedComplaint 
    ? complaints.find(c => c.complaintId === selectedComplaint.complaintId) || selectedComplaint
    : null;

  return (
    <div className="space-y-4">
      {complaints.map((complaint) => (
        <Card
          key={complaint.id}
          className="cursor-pointer hover:shadow-lg transition-shadow bg-white"
          onClick={() => setSelectedComplaint(complaint)}
        >
          <CardContent className="p-6">
            <div className="flex items-start justify-between mb-4">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <span className="font-mono text-sm">{complaint.id}</span>
                  {getStatusBadge(complaint.status)}
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
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Chi tiết khiếu nại</DialogTitle>
          </DialogHeader>

          {activeComplaint && (
            <div className="space-y-6 mt-2">
              <Card>
                <CardContent className="p-4 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs text-muted-foreground">{activeComplaint.id}</span>
                    {getStatusBadge(activeComplaint.status)}
                  </div>
                  <h3 className="text-lg font-bold">{activeComplaint.subject}</h3>
                  <p className="text-sm text-muted-foreground">
                    Sân liên quan: <strong>{activeComplaint.against}</strong>
                  </p>
                  {activeComplaint.bookingId && (
                    <p className="text-xs text-muted-foreground">
                      Mã đặt sân: <strong className="font-mono">BK{String(activeComplaint.bookingId).padStart(6, '0')}</strong>
                    </p>
                  )}
                  <p className="text-sm bg-muted/40 p-3 rounded border text-foreground/80 mt-2">{activeComplaint.description}</p>
                  <div className="text-[10px] text-muted-foreground mt-3">
                    Ngày gửi: {new Date(activeComplaint.submittedDate).toLocaleDateString('vi-VN')}
                  </div>
                </CardContent>
              </Card>

              {/* Responses */}
              {activeComplaint.responses && activeComplaint.responses.length > 0 && (
                <div className="space-y-3">
                  <h4 className="font-bold text-xs text-muted-foreground uppercase">Hộp thư trao đổi:</h4>
                  <div className="space-y-3 max-h-[250px] overflow-y-auto pr-1">
                    {activeComplaint.responses.map((response: ComplaintResponse, idx: number) => {
                      const isMe = isOwner ? (response.from === "Chủ sân") : (response.from === "Khách hàng");
                      return (
                        <div
                          key={idx}
                          className={`flex flex-col max-w-[85%] rounded-lg p-3 ${
                            isMe
                              ? "bg-primary text-primary-foreground ml-auto"
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

              {/* Resolution */}
              {activeComplaint.status === "resolved" && activeComplaint.resolution && (
                <Card className="bg-green-50 border-green-200">
                  <CardHeader className="pb-2">
                    <h4 className="flex items-center gap-2 text-green-800 font-bold text-sm">
                      <AlertCircle className="h-5 w-5 text-green-700" />
                      Đã giải quyết thành công
                    </h4>
                  </CardHeader>
                  <CardContent className="space-y-1">
                    <p className="text-sm font-semibold text-green-700">{activeComplaint.resolution}</p>
                    <p className="text-[10px] text-green-600">
                      Ngày giải quyết: {activeComplaint.resolvedDate ? new Date(activeComplaint.resolvedDate).toLocaleDateString('vi-VN') : ''}
                    </p>
                  </CardContent>
                </Card>
              )}

              {/* Reply / Resolve Actions */}
              {activeComplaint.status !== "resolved" && (
                <div className="space-y-4 pt-2 border-t">
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
                      <Label className="text-green-700">Đóng & Giải quyết khiếu nại</Label>
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
