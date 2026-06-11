'use client'

import { useState, useEffect, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
  Home,
  BarChart3,
  Calendar,
  Star,
  Clock,
  User,
  Send,
  CheckCircle,
  AlertTriangle
} from "lucide-react";
import { useRouter } from "next/navigation";
import { get, post } from "@/lib/api";
import { toast } from "sonner";

type ComplaintResponse = {
  from: string;
  message: string;
  time: string;
};

type Complaint = {
  id: string;
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

const DEFAULT_COMPLAINTS: Complaint[] = [
  {
    id: "CP001",
    subject: "Sân không đúng mô tả",
    against: "Sân bóng Thành Công",
    description: "Sân thực tế không giống hình ảnh trên web. Cỏ nhân tạo cũ, bề mặt không bằng phẳng.",
    status: "open",
    submittedDate: "2026-05-22",
    responses: [],
  },
  {
    id: "CP002",
    subject: "Chủ sân không phản hồi",
    against: "Sân bóng Thành Công",
    description: "Đã liên hệ nhiều lần nhưng chủ sân không phản hồi về việc hoàn tiền do hủy sân.",
    status: "in_progress",
    submittedDate: "2026-05-20",
    responses: [
      {
        from: "Admin",
        message: "Chúng tôi đang xem xét khiếu nại của bạn. Sẽ phản hồi trong 24-48h.",
        time: "2026-05-21 10:00",
      },
    ],
  },
  {
    id: "CP003",
    subject: "Yêu cầu hoàn tiền dịch vụ",
    against: "Sân cầu lông Thành Công",
    description: "Đã hủy sân trước 48h nhưng chưa nhận được tiền hoàn.",
    status: "resolved",
    submittedDate: "2026-05-15",
    resolvedDate: "2026-05-18",
    resolution: "Đã xử lý hoàn tiền 100% vào tài khoản. Vui lòng kiểm tra.",
    responses: [
      {
        from: "Chủ sân",
        message: "Xin lỗi vì sự chậm trễ. Chúng tôi đã xử lý hoàn tiền.",
        time: "2026-05-18 14:00",
      },
    ],
  },
];

function OwnerComplaintsPage() {
  const router = useRouter();
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [replyMessage, setReplyMessage] = useState("");
  const [resolutionText, setResolutionText] = useState("");
  const [showResolveDialog, setShowResolveDialog] = useState(false);
  const [filterStatus, setFilterStatus] = useState("all");

  const fetchComplaints = useCallback(async () => {
    try {
      const data = await get<Complaint[]>("/owner/complaints");
      if (data && Array.isArray(data)) {
        setComplaints(data);
        localStorage.setItem('sport_venue_complaints', JSON.stringify(data));
        setSelectedComplaint(prev => {
          if (prev) {
            return data.find(c => String(c.id) === String(prev.id)) || null;
          }
          return null;
        });
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

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  const saveComplaints = (updated: Complaint[]) => {
    setComplaints(updated);
    localStorage.setItem('sport_venue_complaints', JSON.stringify(updated));
    setSelectedComplaint(prev => {
      if (prev) {
        return updated.find(c => String(c.id) === String(prev.id)) || null;
      }
      return null;
    });
  };

  const handleSendMessage = async () => {
    if (!replyMessage.trim() || !selectedComplaint) return;

    try {
      const cleanId = String(selectedComplaint.id).replace("CP", "");
      await post<unknown>(`/owner/complaints/${cleanId}/reply`, { message: replyMessage.trim() });
      setReplyMessage("");
      toast.success("Gửi phản hồi thành công!");
      fetchComplaints();
    } catch (error) {
      console.warn("Backend reply failed, using local update:", error);
      const newResponse = {
        from: "Chủ sân",
        message: replyMessage.trim(),
        time: new Date().toLocaleString('vi-VN')
      };

      const updated = complaints.map(c => {
        if (String(c.id) === String(selectedComplaint.id)) {
          const currentStatus = (c.status || "").toLowerCase();
          return {
            ...c,
            status: currentStatus === "open" ? "in_progress" : c.status,
            responses: [...(c.responses || []), newResponse]
          };
        }
        return c;
      });

      saveComplaints(updated);
      setReplyMessage("");
      toast.success("Đã phản hồi (Offline Mode)");
    }
  };

  const handleResolveSubmit = async () => {
    if (!resolutionText.trim() || !selectedComplaint) return;

    try {
      const cleanId = String(selectedComplaint.id).replace("CP", "");
      await post<unknown>(`/owner/complaints/${cleanId}/resolve`, { resolution: resolutionText.trim() });
      setResolutionText("");
      setShowResolveDialog(false);
      toast.success("Giải quyết khiếu nại thành công!");
      fetchComplaints();
    } catch (error) {
      console.warn("Backend resolve failed, using local update:", error);
      const updated = complaints.map(c => {
        if (String(c.id) === String(selectedComplaint.id)) {
          return {
            ...c,
            status: "resolved",
            resolution: resolutionText.trim(),
            resolvedDate: new Date().toISOString().split('T')[0]
          };
        }
        return c;
      });

      saveComplaints(updated);
      setResolutionText("");
      setShowResolveDialog(false);
      toast.success("Đã giải quyết khiếu nại (Offline Mode)");
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

  const getFilteredComplaints = () => {
    if (filterStatus === "all") return complaints;
    return complaints.filter(c => c.status === filterStatus);
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="flex">
        {/* Sidebar */}
        <aside className="w-64 min-h-[calc(100vh-64px)] bg-card border-r p-4 shrink-0">
          <h2 className="text-xl font-bold mb-6 px-3 text-primary">Quản lý chủ sân</h2>
          <nav className="space-y-1">
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/dashboard")}
            >
              <Home className="mr-3 h-4 w-4" />
              Dashboard
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/venues")}
            >
              <BarChart3 className="mr-3 h-4 w-4" />
              Sân của tôi
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/bookings")}
            >
              <Calendar className="mr-3 h-4 w-4" />
              Lịch đặt sân
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/reviews")}
            >
              <Star className="mr-3 h-4 w-4" />
              Đánh giá của khách
            </Button>
            <Button
              variant="default"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/complaints")}
            >
              <AlertCircle className="mr-3 h-4 w-4" />
              Khiếu nại khách hàng
            </Button>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 p-8 bg-muted/10">
          <div className="flex justify-between items-center mb-8">
            <h1 className="text-3xl font-bold">Quản lý khiếu nại</h1>
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger className="w-48">
                <SelectValue placeholder="Tất cả trạng thái" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả khiếu nại</SelectItem>
                <SelectItem value="open">Mới</SelectItem>
                <SelectItem value="in_progress">Đang xử lý</SelectItem>
                <SelectItem value="resolved">Đã giải quyết</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left list column */}
            <div className="lg:col-span-1 space-y-4">
              {getFilteredComplaints().map((c) => (
                <Card
                  key={c.id}
                  className={`cursor-pointer hover:shadow transition-shadow border-l-4 ${
                    selectedComplaint?.id === c.id
                      ? "border-l-primary bg-primary/5"
                      : c.status === "open"
                      ? "border-l-yellow-500"
                      : c.status === "in_progress"
                      ? "border-l-blue-500"
                      : "border-l-green-500"
                  }`}
                  onClick={() => setSelectedComplaint(c)}
                >
                  <CardContent className="p-4 space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-xs font-mono text-muted-foreground">{c.id}</span>
                      {getStatusBadge(c.status)}
                    </div>
                    <h3 className="font-semibold text-sm line-clamp-1">{c.subject}</h3>
                    <p className="text-xs text-muted-foreground">Đối tượng: {c.against}</p>
                    <p className="text-xs text-muted-foreground">{new Date(c.submittedDate).toLocaleDateString('vi-VN')}</p>
                  </CardContent>
                </Card>
              ))}

              {getFilteredComplaints().length === 0 && (
                <div className="text-center p-8 text-muted-foreground">
                  Không tìm thấy khiếu nại nào.
                </div>
              )}
            </div>

            {/* Right details column */}
            <div className="lg:col-span-2">
              {selectedComplaint ? (
                <Card className="flex flex-col h-[calc(100vh-220px)]">
                  {/* Detail Header */}
                  <CardHeader className="border-b bg-card flex flex-row justify-between items-center py-4">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-mono text-xs text-muted-foreground">{selectedComplaint.id}</span>
                        {getStatusBadge(selectedComplaint.status)}
                      </div>
                      <h2 className="text-lg font-bold text-foreground">{selectedComplaint.subject}</h2>
                    </div>

                    {selectedComplaint.status !== "resolved" && (
                      <Button
                        size="sm"
                        className="bg-green-600 hover:bg-green-700 text-white"
                        onClick={() => setShowResolveDialog(true)}
                      >
                        <CheckCircle className="h-4 w-4 mr-2" />
                        Giải quyết
                      </Button>
                    )}
                  </CardHeader>

                  {/* Detail Content & Message list */}
                  <CardContent className="flex-1 overflow-y-auto p-6 space-y-6 bg-muted/5">
                    {/* Customer description */}
                    <div className="bg-card border rounded-lg p-4 space-y-2 shadow-sm">
                      <div className="flex justify-between items-center text-xs text-muted-foreground">
                        <span className="font-bold flex items-center gap-1"><User className="h-3.5 w-3.5" /> Khách hàng</span>
                        <span>{new Date(selectedComplaint.submittedDate).toLocaleDateString('vi-VN')}</span>
                      </div>
                      <p className="text-sm font-medium">{selectedComplaint.description}</p>
                    </div>

                    {/* Chat replies */}
                    {selectedComplaint.responses.length > 0 && (
                      <div className="space-y-4">
                        <h4 className="text-xs font-bold text-muted-foreground uppercase">Lịch sử phản hồi:</h4>
                        {selectedComplaint.responses.map((res: ComplaintResponse, idx: number) => (
                          <div
                            key={idx}
                            className={`flex flex-col max-w-[85%] rounded-lg p-3 ${
                              res.from === "Chủ sân"
                                ? "bg-primary text-primary-foreground ml-auto"
                                : "bg-card border mr-auto"
                            }`}
                          >
                            <span className="text-[10px] font-bold opacity-85 mb-1">{res.from}</span>
                            <p className="text-sm font-medium leading-relaxed">{res.message}</p>
                            <span className="text-[9px] opacity-75 self-end mt-1">{res.time}</span>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Resolution Statement */}
                    {selectedComplaint.status === "resolved" && selectedComplaint.resolution && (
                      <div className="bg-green-50 border border-green-200 rounded-lg p-4 space-y-2">
                        <h4 className="flex items-center gap-2 text-green-800 font-bold text-sm">
                          <CheckCircle className="h-5 w-5 text-green-700" />
                          Đã thống nhất giải quyết thành công
                        </h4>
                        <p className="text-sm text-green-700 font-semibold">{selectedComplaint.resolution}</p>
                        <p className="text-[10px] text-green-600">Ngày giải quyết: {new Date(selectedComplaint.resolvedDate).toLocaleDateString('vi-VN')}</p>
                      </div>
                    )}
                  </CardContent>

                  {/* Send panel */}
                  {selectedComplaint.status !== "resolved" && (
                    <div className="border-t bg-card p-4 flex gap-2">
                      <Textarea
                        placeholder="Viết tin nhắn phản hồi..."
                        value={replyMessage}
                        onChange={(e) => setReplyMessage(e.target.value)}
                        rows={1}
                        className="flex-1 min-h-[40px] resize-none"
                      />
                      <Button
                        className="bg-primary hover:bg-primary/95 text-white h-10"
                        disabled={!replyMessage.trim()}
                        onClick={handleSendMessage}
                      >
                        <Send className="h-4 w-4 mr-2" />
                        Gửi
                      </Button>
                    </div>
                  )}
                </Card>
              ) : (
                <Card className="flex flex-col items-center justify-center p-12 text-muted-foreground h-[calc(100vh-220px)] bg-card border-dashed">
                  <AlertCircle className="h-12 w-12 mb-3 opacity-30" />
                  <p>Chọn một khiếu nại ở danh sách bên trái để bắt đầu xử lý</p>
                </Card>
              )}
            </div>
          </div>
        </main>
      </div>

      {/* Resolve Dialog */}
      <Dialog open={showResolveDialog} onOpenChange={setShowResolveDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác nhận giải quyết khiếu nại</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="bg-yellow-50 border border-yellow-200 rounded p-3 flex gap-2 text-yellow-800 text-xs">
              <AlertTriangle className="h-5 w-5 shrink-0" />
              <span>Khi giải quyết khiếu nại, trạng thái sẽ chuyển thành <strong>Đã giải quyết</strong> và cuộc hội thoại sẽ kết thúc.</span>
            </div>
            <div className="space-y-2">
              <Label>Mô tả giải pháp xử lý thực tế *</Label>
              <Textarea
                placeholder="VD: Đã hoàn tiền 100% qua ví điện tử của khách hàng..."
                value={resolutionText}
                onChange={(e) => setResolutionText(e.target.value)}
                rows={4}
              />
            </div>
            <div className="flex gap-2 justify-end">
              <Button variant="outline" onClick={() => setShowResolveDialog(false)}>
                Hủy
              </Button>
              <Button
                className="bg-green-600 hover:bg-green-700 text-white"
                disabled={!resolutionText.trim()}
                onClick={handleResolveSubmit}
              >
                Giải quyết khiếu nại
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default OwnerComplaintsPage;
