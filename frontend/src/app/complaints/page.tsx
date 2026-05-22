'use client'

import { useState } from "react";
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
import { AlertCircle, Plus, MessageSquare } from "lucide-react";

function ComplaintsPage() {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState<any>(null);

  const complaints = [
    {
      id: "CP001",
      subject: "Sân không đúng mô tả",
      against: "Sân bóng Thành Công",
      description: "Sân thực tế không giống hình ảnh trên web. Cỏ nhân tạo cũ, bề mặt không bằng phẳng.",
      status: "open",
      submittedDate: "22/05/2024",
      responses: [],
    },
    {
      id: "CP002",
      subject: "Chủ sân không phản hồi",
      against: "Arena Sports Center",
      description: "Đã liên hệ nhiều lần nhưng chủ sân không phản hồi về việc hoàn tiền do hủy sân.",
      status: "in_progress",
      submittedDate: "20/05/2024",
      responses: [
        {
          from: "Admin",
          message: "Chúng tôi đang xem xét khiếu nại của bạn. Sẽ phản hồi trong 24-48h.",
          time: "21/05/2024 10:00",
        },
      ],
    },
    {
      id: "CP003",
      subject: "Yêu cầu hoàn tiền",
      against: "Sân Vận Động Quận 7",
      description: "Đã hủy sân trước 48h nhưng chưa nhận được tiền hoàn.",
      status: "resolved",
      submittedDate: "15/05/2024",
      resolvedDate: "18/05/2024",
      resolution: "Đã xử lý hoàn tiền 100% vào tài khoản. Vui lòng kiểm tra.",
      responses: [
        {
          from: "Chủ sân",
          message: "Xin lỗi vì sự chậm trễ. Chúng tôi đã xử lý hoàn tiền.",
          time: "18/05/2024 14:00",
        },
      ],
    },
  ];

  const getStatusBadge = (status: string) => {
    const config = {
      open: { label: "Mới", className: "bg-yellow-100 text-yellow-700" },
      in_progress: { label: "Đang xử lý", className: "bg-blue-100 text-blue-700" },
      resolved: { label: "Đã giải quyết", className: "bg-green-100 text-green-700" },
    };
    const item = config[status as keyof typeof config];
    return <Badge className={item.className}>{item.label}</Badge>;
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-center justify-between mb-8">
            <h1 className="text-3xl">Khiếu nại của tôi</h1>
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
                      <h3 className="mb-1">{complaint.subject}</h3>
                      <p className="text-sm text-muted-foreground mb-2">
                        Khiếu nại về: <strong>{complaint.against}</strong>
                      </p>
                      <p className="text-sm text-muted-foreground line-clamp-2">
                        {complaint.description}
                      </p>
                    </div>
                    <div className="text-sm text-muted-foreground">
                      {complaint.submittedDate}
                    </div>
                  </div>

                  {complaint.responses.length > 0 && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <MessageSquare className="h-4 w-4" />
                      <span>{complaint.responses.length} phản hồi</span>
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
              <Select>
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
              <Input id="booking" placeholder="VD: BK001234" />
            </div>

            <div className="space-y-2">
              <Label htmlFor="subject">Tiêu đề khiếu nại *</Label>
              <Input id="subject" placeholder="Vấn đề bạn gặp phải..." />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Mô tả chi tiết *</Label>
              <Textarea
                id="description"
                placeholder="Mô tả chi tiết vấn đề, điều bạn mong muốn..."
                rows={6}
              />
            </div>

            <div className="space-y-2">
              <Label>Ảnh minh chứng (nếu có)</Label>
              <div className="border-2 border-dashed rounded-lg p-8 text-center">
                <Button variant="outline">
                  <Plus className="mr-2 h-4 w-4" />
                  Tải ảnh lên
                </Button>
                <p className="text-sm text-muted-foreground mt-2">
                  Tối đa 5 ảnh
                </p>
              </div>
            </div>

            <div className="flex gap-2">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => setShowCreateDialog(false)}
              >
                Hủy
              </Button>
              <Button className="flex-1">Gửi khiếu nại</Button>
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

          {selectedComplaint && (
            <div className="space-y-6">
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="font-mono text-sm">{selectedComplaint.id}</span>
                    {getStatusBadge(selectedComplaint.status)}
                  </div>
                  <h3 className="mb-2">{selectedComplaint.subject}</h3>
                  <p className="text-sm text-muted-foreground mb-3">
                    Khiếu nại về: <strong>{selectedComplaint.against}</strong>
                  </p>
                  <p className="text-sm">{selectedComplaint.description}</p>
                  <div className="text-xs text-muted-foreground mt-3">
                    Ngày gửi: {selectedComplaint.submittedDate}
                  </div>
                </CardContent>
              </Card>

              {/* Responses */}
              {selectedComplaint.responses.length > 0 && (
                <div>
                  <h4 className="mb-3">Phản hồi</h4>
                  <div className="space-y-3">
                    {selectedComplaint.responses.map((response: any, idx: number) => (
                      <Card key={idx}>
                        <CardContent className="p-4">
                          <div className="flex items-center justify-between mb-2">
                            <strong className="text-sm">{response.from}</strong>
                            <span className="text-xs text-muted-foreground">
                              {response.time}
                            </span>
                          </div>
                          <p className="text-sm">{response.message}</p>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>
              )}

              {/* Resolution */}
              {selectedComplaint.status === "resolved" && selectedComplaint.resolution && (
                <Card className="bg-green-50 border-green-200">
                  <CardHeader>
                    <h4 className="flex items-center gap-2">
                      <AlertCircle className="h-5 w-5 text-green-700" />
                      Đã giải quyết
                    </h4>
                  </CardHeader>
                  <CardContent>
                    <p className="text-sm">{selectedComplaint.resolution}</p>
                    <p className="text-xs text-muted-foreground mt-2">
                      Ngày giải quyết: {selectedComplaint.resolvedDate}
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
