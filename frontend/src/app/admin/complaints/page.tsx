'use client'

import { useState } from "react";

import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { AlertCircle, MessageSquare, Image as ImageIcon } from "lucide-react";

function ComplaintsManagementPage() {
  const [selectedComplaint, setSelectedComplaint] = useState<any>(null);
  const [resolution, setResolution] = useState("");

  const complaints = [
    {
      id: "CP001",
      complainant: {
        name: "Nguyễn Văn A",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
      },
      against: {
        name: "Sân bóng Thành Công",
        type: "owner",
      },
      subject: "Sân không đúng mô tả",
      description:
        "Sân thực tế không giống như hình ảnh trên web. Cỏ nhân tạo cũ, bề mặt không bằng phẳng.",
      submittedDate: "22/05/2024",
      priority: "high",
      status: "open",
      evidence: ["photo1.jpg", "photo2.jpg"],
    },
    {
      id: "CP002",
      complainant: {
        name: "Trần Thị B",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=2",
      },
      against: {
        name: "Chủ sân Arena",
        type: "owner",
      },
      subject: "Chủ sân không phản hồi",
      description:
        "Đã liên hệ nhiều lần nhưng chủ sân không phản hồi về việc hoàn tiền do hủy sân.",
      submittedDate: "21/05/2024",
      priority: "medium",
      status: "in_progress",
      evidence: [],
    },
    {
      id: "CP003",
      complainant: {
        name: "Lê Văn C",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=3",
      },
      against: {
        name: "Khách hàng D",
        type: "customer",
      },
      subject: "Khách hàng gây rối",
      description: "Khách hàng có hành vi thiếu văn hóa, gây ồn ào.",
      submittedDate: "20/05/2024",
      priority: "low",
      status: "resolved",
      evidence: [],
      resolution: "Đã cảnh cáo khách hàng và tạm khóa tài khoản 7 ngày.",
    },
  ];

  const getPriorityBadge = (priority: string) => {
    const config = {
      high: { label: "Cao", className: "bg-red-100 text-red-700" },
      medium: { label: "Trung bình", className: "bg-yellow-100 text-yellow-700" },
      low: { label: "Thấp", className: "bg-blue-100 text-blue-700" },
    };
    const item = config[priority as keyof typeof config];
    return <Badge className={item.className}>{item.label}</Badge>;
  };

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
    <div className="p-8">
      <div className="container mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl">Quản lý khiếu nại</h1>

          <div className="flex gap-4">
            <Select defaultValue="all">
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả trạng thái</SelectItem>
                <SelectItem value="open">Mới</SelectItem>
                <SelectItem value="in_progress">Đang xử lý</SelectItem>
                <SelectItem value="resolved">Đã giải quyết</SelectItem>
              </SelectContent>
            </Select>

            <Select defaultValue="all-priority">
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all-priority">Tất cả mức độ</SelectItem>
                <SelectItem value="high">Cao</SelectItem>
                <SelectItem value="medium">Trung bình</SelectItem>
                <SelectItem value="low">Thấp</SelectItem>
              </SelectContent>
            </Select>
          </div>
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
                  <div className="flex items-start gap-4 flex-1">
                    <Avatar>
                      <AvatarImage src={complaint.complainant.avatar} />
                      <AvatarFallback>
                        {complaint.complainant.name[0]}
                      </AvatarFallback>
                    </Avatar>

                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="font-mono text-sm">{complaint.id}</span>
                        {getPriorityBadge(complaint.priority)}
                        {getStatusBadge(complaint.status)}
                      </div>

                      <h3 className="mb-2">{complaint.subject}</h3>

                      <div className="text-sm text-muted-foreground mb-3">
                        <strong>{complaint.complainant.name}</strong> khiếu nại về{" "}
                        <strong>{complaint.against.name}</strong> ({complaint.against.type === "owner" ? "Chủ sân" : "Khách hàng"})
                      </div>

                      <p className="text-sm text-muted-foreground line-clamp-2">
                        {complaint.description}
                      </p>

                      {complaint.evidence.length > 0 && (
                        <div className="flex items-center gap-2 mt-3">
                          <ImageIcon className="h-4 w-4 text-muted-foreground" />
                          <span className="text-sm text-muted-foreground">
                            {complaint.evidence.length} ảnh minh chứng
                          </span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="text-sm text-muted-foreground">
                    {complaint.submittedDate}
                  </div>
                </div>

                {complaint.status !== "resolved" && (
                  <div className="flex gap-2 justify-end">
                    <Button size="sm" variant="outline">
                      Xem chi tiết
                    </Button>
                    {complaint.status === "open" && (
                      <Button size="sm" variant="default">
                        Bắt đầu xử lý
                      </Button>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Complaint Detail Sheet */}
        <Sheet
          open={!!selectedComplaint}
          onOpenChange={() => setSelectedComplaint(null)}
        >
          <SheetContent className="w-full sm:max-w-2xl overflow-y-auto">
            <SheetHeader>
              <SheetTitle>Chi tiết khiếu nại</SheetTitle>
            </SheetHeader>

            {selectedComplaint && (
              <div className="mt-6 space-y-6">
                {/* Header */}
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center gap-2 mb-3">
                      <span className="font-mono text-sm">
                        {selectedComplaint.id}
                      </span>
                      {getPriorityBadge(selectedComplaint.priority)}
                      {getStatusBadge(selectedComplaint.status)}
                    </div>
                    <h3 className="mb-2">{selectedComplaint.subject}</h3>
                    <div className="text-sm text-muted-foreground">
                      Ngày gửi: {selectedComplaint.submittedDate}
                    </div>
                  </CardContent>
                </Card>

                {/* Parties */}
                <div className="grid grid-cols-2 gap-4">
                  <Card>
                    <CardHeader>
                      <h4 className="text-sm">Người khiếu nại</h4>
                    </CardHeader>
                    <CardContent>
                      <div className="flex items-center gap-3">
                        <Avatar>
                          <AvatarImage src={selectedComplaint.complainant.avatar} />
                          <AvatarFallback>
                            {selectedComplaint.complainant.name[0]}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <div>{selectedComplaint.complainant.name}</div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader>
                      <h4 className="text-sm">Bị khiếu nại</h4>
                    </CardHeader>
                    <CardContent>
                      <div>
                        <div>{selectedComplaint.against.name}</div>
                        <Badge variant="outline" className="mt-2">
                          {selectedComplaint.against.type === "owner"
                            ? "Chủ sân"
                            : "Khách hàng"}
                        </Badge>
                      </div>
                    </CardContent>
                  </Card>
                </div>

                {/* Description */}
                <Card>
                  <CardHeader>
                    <h4>Nội dung khiếu nại</h4>
                  </CardHeader>
                  <CardContent>
                    <p className="text-sm">{selectedComplaint.description}</p>
                  </CardContent>
                </Card>

                {/* Evidence */}
                {selectedComplaint.evidence.length > 0 && (
                  <Card>
                    <CardHeader>
                      <h4>Minh chứng ({selectedComplaint.evidence.length})</h4>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-2 gap-2">
                        {selectedComplaint.evidence.map((file: string, idx: number) => (
                          <div
                            key={idx}
                            className="aspect-video bg-muted rounded-lg flex items-center justify-center"
                          >
                            <ImageIcon className="h-8 w-8 text-muted-foreground" />
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                )}

                {/* Resolution */}
                {selectedComplaint.status === "resolved" ? (
                  <Card className="bg-green-50 border-green-200">
                    <CardHeader>
                      <h4 className="flex items-center gap-2">
                        <AlertCircle className="h-5 w-5 text-green-700" />
                        Đã giải quyết
                      </h4>
                    </CardHeader>
                    <CardContent>
                      <p className="text-sm">{selectedComplaint.resolution}</p>
                    </CardContent>
                  </Card>
                ) : (
                  <Card>
                    <CardHeader>
                      <h4>Giải quyết khiếu nại</h4>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <Textarea
                        placeholder="Nhập giải pháp xử lý..."
                        value={resolution}
                        onChange={(e) => setResolution(e.target.value)}
                        rows={4}
                      />
                      <div className="flex gap-2">
                        <Button
                          variant="default"
                          className="flex-1"
                          disabled={!resolution}
                        >
                          <MessageSquare className="h-4 w-4 mr-2" />
                          Gửi phản hồi
                        </Button>
                        <Button
                          variant="outline"
                          className="flex-1"
                          disabled={!resolution}
                        >
                          Đánh dấu đã giải quyết
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                )}
              </div>
            )}
          </SheetContent>
        </Sheet>
      </div>
    </div>
  );
}

export default ComplaintsManagementPage;
