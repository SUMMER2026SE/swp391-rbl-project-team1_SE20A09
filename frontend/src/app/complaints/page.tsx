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
import { AlertCircle, Plus, MessageSquare, User } from "lucide-react";
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

function ComplaintsPage() {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);

  // Form states
  const [against, setAgainst] = useState("1");
  const [bookingId, setBookingId] = useState("");
  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");

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

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  const handleCreateComplaint = async () => {
    if (!subject.trim() || !description.trim()) return;

    // Derive a booking ID number if provided
    const numericBookingId = bookingId.trim() ? Number(bookingId.trim().replace(/\D/g, '')) : null;

    try {
      if (!numericBookingId) {
        throw new Error("Vui lòng nhập Mã đơn đặt sân hợp lệ (dạng số)");
      }
      await post<unknown>("/complaints", {
        bookingId: numericBookingId,
        subject: subject.trim(),
        description: description.trim()
      });
      toast.success("Gửi khiếu nại thành công!");
      fetchComplaints();
    } catch (error: unknown) {
      console.warn("Backend create complaint failed, using local update:", error);
      let targetVenue = "Sân bóng Thành Công";
      if (against === "2") targetVenue = "Arena Sports Center";
      if (against === "3") targetVenue = "Sân Vận Động Quận 7";

      const newId = `CP00${complaints.length + 1}`;
      const newComplaint = {
        id: newId,
        subject: subject.trim(),
        against: targetVenue,
        description: description.trim(),
        status: "open",
        submittedDate: new Date().toISOString().split('T')[0],
        responses: [],
        bookingId: bookingId.trim() || undefined
      };

      const updated = [newComplaint, ...complaints];
      setComplaints(updated);
      localStorage.setItem('sport_venue_complaints', JSON.stringify(updated));
      toast.success("Đã lưu khiếu nại (Offline Mode)");
    }

    // Reset Form
    setSubject("");
    setDescription("");
    setBookingId("");
    setAgainst("1");
    setShowCreateDialog(false);
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

  // Sync selected complaint details if it updates in the list
  const activeComplaint = selectedComplaint 
    ? complaints.find(c => c.id === selectedComplaint.id) || selectedComplaint
    : null;

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-center justify-between mb-8">
            <h1 className="text-3xl font-bold">Khiếu nại của tôi</h1>
            <Button onClick={() => setShowCreateDialog(true)}>
              <Plus className="mr-2 h-5 w-5" />
              Tạo khiếu nại
            </Button>
          </div>

          <div className="space-y-4">
            {complaints.map((complaint) => (
              <Card
                key={complaint.id}
                className="cursor-pointer hover:shadow-lg transition-shadow"
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
                        Khiếu nại về: <strong>{complaint.against}</strong>
                      </p>
                      <p className="text-sm text-muted-foreground line-clamp-2">
                        {complaint.description}
                      </p>
                    </div>
                    <div className="text-sm text-muted-foreground">
                      {new Date(complaint.submittedDate).toLocaleDateString('vi-VN')}
                    </div>
                  </div>

                  {complaint.responses.length > 0 && (
                    <div className="flex items-center gap-2 text-sm text-primary font-medium">
                      <MessageSquare className="h-4 w-4" />
                      <span>{complaint.responses.length} phản hồi từ chủ sân/admin</span>
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}

            {complaints.length === 0 && (
              <Card>
                <CardContent className="p-12 text-center text-muted-foreground">
                  <AlertCircle className="h-12 w-12 mx-auto mb-3 opacity-50" />
                  <p>Bạn chưa có khiếu nại nào</p>
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
            <DialogTitle>Tạo khiếu nại mới</DialogTitle>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="venue">Sân/Chủ sân *</Label>
              <Select value={against} onValueChange={setAgainst}>
                <SelectTrigger id="venue">
                  <SelectValue placeholder="Chọn sân hoặc chủ sân" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">Sân bóng Thành Công</SelectItem>
                  <SelectItem value="2">Arena Sports Center</SelectItem>
                  <SelectItem value="3">Sân Vận Động Quận 7</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="booking">Mã đặt sân liên quan (nếu có)</Label>
              <Input 
                id="booking" 
                placeholder="VD: BK001234" 
                value={bookingId}
                onChange={(e) => setBookingId(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="subject">Tiêu đề khiếu nại *</Label>
              <Input 
                id="subject" 
                placeholder="Vấn đề bạn gặp phải..." 
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Mô tả chi tiết *</Label>
              <Textarea
                id="description"
                placeholder="Mô tả chi tiết vấn đề, điều bạn mong muốn..."
                rows={6}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <div className="flex gap-2 pt-2">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => setShowCreateDialog(false)}
              >
                Hủy
              </Button>
              <Button 
                className="flex-1 bg-primary hover:bg-primary/95 text-white"
                disabled={!subject.trim() || !description.trim()}
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
        onOpenChange={() => setSelectedComplaint(null)}
      >
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
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
                    Khiếu nại về: <strong>{activeComplaint.against}</strong>
                  </p>
                  {activeComplaint.bookingId && (
                    <p className="text-xs text-muted-foreground">
                      Mã đặt sân liên quan: <strong className="font-mono">{activeComplaint.bookingId}</strong>
                    </p>
                  )}
                  <p className="text-sm bg-muted/40 p-3 rounded border text-foreground/80 mt-2">{activeComplaint.description}</p>
                  <div className="text-[10px] text-muted-foreground mt-3">
                    Ngày gửi: {new Date(activeComplaint.submittedDate).toLocaleDateString('vi-VN')}
                  </div>
                </CardContent>
              </Card>

              {/* Responses */}
              {activeComplaint.responses.length > 0 && (
                <div className="space-y-3">
                  <h4 className="font-bold text-xs text-muted-foreground uppercase">Hộp thư trao đổi:</h4>
                  <div className="space-y-3 max-h-[250px] overflow-y-auto pr-1">
                    {activeComplaint.responses.map((response: ComplaintResponse, idx: number) => (
                      <div
                        key={idx}
                        className={`flex flex-col max-w-[85%] rounded-lg p-3 ${
                          response.from === "Chủ sân" || response.from === "Admin"
                            ? "bg-muted text-foreground mr-auto border"
                            : "bg-primary text-primary-foreground ml-auto"
                        }`}
                      >
                        <strong className="text-[10px] opacity-85 mb-1">{response.from}</strong>
                        <p className="text-sm leading-relaxed">{response.message}</p>
                        <span className="text-[9px] opacity-75 mt-1 self-end">{response.time}</span>
                      </div>
                    ))}
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
                      Ngày giải quyết: {new Date(activeComplaint.resolvedDate as string).toLocaleDateString('vi-VN')}
                    </p>
                  </CardContent>
                </Card>
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
