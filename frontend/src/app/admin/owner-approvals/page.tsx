'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CheckCircle, XCircle, FileText, MapPin, Building } from "lucide-react";

export function OwnerApprovalPage() {
  const [selectedApplication, setSelectedApplication] = useState<any>(null);
  const [rejectReason, setRejectReason] = useState("");

  const applications = [
    {
      id: 1,
      owner: {
        name: "Nguyễn Văn A",
        email: "owner1@email.com",
        phone: "0901234567",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=owner1",
      },
      venue: {
        name: "Sân bóng Phú Nhuận",
        address: "123 Đường ABC, Phú Nhuận, TP.HCM",
        sportType: "Bóng đá",
        description: "Sân bóng chất lượng cao với cỏ nhân tạo...",
      },
      documents: [
        "Chứng minh nhân dân",
        "Giấy phép kinh doanh",
        "Hợp đồng thuê mặt bằng",
      ],
      submittedDate: "20/05/2024",
      status: "pending",
    },
    {
      id: 2,
      owner: {
        name: "Trần Thị B",
        email: "owner2@email.com",
        phone: "0909876543",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=owner2",
      },
      venue: {
        name: "Sân cầu lông Quận 3",
        address: "456 Đường XYZ, Quận 3, TP.HCM",
        sportType: "Cầu lông",
        description: "Hệ thống 4 sân cầu lông tiêu chuẩn thi đấu...",
      },
      documents: [
        "Chứng minh nhân dân",
        "Giấy phép kinh doanh",
        "Hợp đồng thuê mặt bằng",
      ],
      submittedDate: "21/05/2024",
      status: "pending",
    },
  ];

  const approvedApplications = [
    {
      id: 3,
      owner: {
        name: "Lê Văn C",
        email: "owner3@email.com",
        phone: "0903456789",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=owner3",
      },
      venue: {
        name: "Sân quần vợt Quận 7",
        address: "789 Đường DEF, Quận 7, TP.HCM",
        sportType: "Quần vợt",
      },
      submittedDate: "15/05/2024",
      approvedDate: "18/05/2024",
      status: "approved",
    },
  ];

  const rejectedApplications = [
    {
      id: 4,
      owner: {
        name: "Phạm Thị D",
        email: "owner4@email.com",
        phone: "0904567890",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=owner4",
      },
      venue: {
        name: "Sân bóng rổ Bình Thạnh",
        address: "321 Đường GHI, Bình Thạnh, TP.HCM",
        sportType: "Bóng rổ",
      },
      submittedDate: "12/05/2024",
      rejectedDate: "14/05/2024",
      rejectionReason: "Giấy tờ không đầy đủ",
      status: "rejected",
    },
  ];

  const getStatusBadge = (status: string) => {
    const config = {
      pending: { label: "Chờ duyệt", className: "bg-yellow-100 text-yellow-700" },
      approved: { label: "Đã duyệt", className: "bg-green-100 text-green-700" },
      rejected: { label: "Từ chối", className: "bg-red-100 text-red-700" },
    };
    const item = config[status as keyof typeof config];
    return <Badge className={item.className}>{item.label}</Badge>;
  };

  const ApplicationCard = ({ application }: { application: any }) => (
    <Card className="mb-4">
      <CardContent className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-start gap-4">
            <Avatar className="h-12 w-12">
              <AvatarImage src={application.owner.avatar} />
              <AvatarFallback>{application.owner.name[0]}</AvatarFallback>
            </Avatar>
            <div>
              <h3 className="mb-1">{application.owner.name}</h3>
              <div className="text-sm text-muted-foreground mb-2">
                {application.owner.email} • {application.owner.phone}
              </div>
              {getStatusBadge(application.status)}
            </div>
          </div>
          <div className="text-sm text-muted-foreground">
            Ngày nộp: {application.submittedDate}
          </div>
        </div>

        <div className="bg-muted/50 rounded-lg p-4 mb-4">
          <div className="flex items-center gap-2 mb-3">
            <Building className="h-5 w-5 text-primary" />
            <h4>Thông tin sân</h4>
          </div>
          <div className="space-y-2 text-sm">
            <div>
              <span className="text-muted-foreground">Tên sân:</span>{" "}
              {application.venue.name}
            </div>
            <div className="flex items-start gap-2">
              <MapPin className="h-4 w-4 mt-0.5 text-muted-foreground" />
              <span>{application.venue.address}</span>
            </div>
            <div>
              <span className="text-muted-foreground">Loại:</span>{" "}
              <Badge variant="outline">{application.venue.sportType}</Badge>
            </div>
            {application.venue.description && (
              <div>
                <span className="text-muted-foreground">Mô tả:</span>{" "}
                {application.venue.description}
              </div>
            )}
          </div>
        </div>

        {application.documents && (
          <div className="mb-4">
            <div className="flex items-center gap-2 mb-2">
              <FileText className="h-4 w-4 text-primary" />
              <h4 className="text-sm">Tài liệu đính kèm</h4>
            </div>
            <div className="flex flex-wrap gap-2">
              {application.documents.map((doc: string, idx: number) => (
                <Badge key={idx} variant="outline">
                  {doc}
                </Badge>
              ))}
            </div>
          </div>
        )}

        {application.status === "pending" && (
          <div className="flex gap-2">
            <Button
              variant="outline"
              className="flex-1"
              onClick={() => setSelectedApplication(application)}
            >
              Xem chi tiết
            </Button>
            <Button variant="default" className="flex-1">
              <CheckCircle className="h-4 w-4 mr-2" />
              Phê duyệt
            </Button>
            <Button variant="destructive" className="flex-1">
              <XCircle className="h-4 w-4 mr-2" />
              Từ chối
            </Button>
          </div>
        )}

        {application.status === "rejected" && application.rejectionReason && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <div className="text-sm">
              <strong>Lý do từ chối:</strong> {application.rejectionReason}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl mb-8">Duyệt chủ sân</h1>

        <Tabs defaultValue="pending">
          <TabsList className="mb-6">
            <TabsTrigger value="pending">
              Chờ duyệt ({applications.length})
            </TabsTrigger>
            <TabsTrigger value="approved">
              Đã duyệt ({approvedApplications.length})
            </TabsTrigger>
            <TabsTrigger value="rejected">
              Từ chối ({rejectedApplications.length})
            </TabsTrigger>
          </TabsList>

          <TabsContent value="pending">
            {applications.map((app) => (
              <ApplicationCard key={app.id} application={app} />
            ))}
          </TabsContent>

          <TabsContent value="approved">
            {approvedApplications.map((app) => (
              <ApplicationCard key={app.id} application={app} />
            ))}
          </TabsContent>

          <TabsContent value="rejected">
            {rejectedApplications.map((app) => (
              <ApplicationCard key={app.id} application={app} />
            ))}
          </TabsContent>
        </Tabs>

        {/* Reject Dialog */}
        <Dialog
          open={!!selectedApplication}
          onOpenChange={() => setSelectedApplication(null)}
        >
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Từ chối đơn đăng ký</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <label className="block mb-2">Lý do từ chối *</label>
                <Textarea
                  placeholder="Nhập lý do từ chối..."
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  rows={4}
                />
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => setSelectedApplication(null)}
                >
                  Hủy
                </Button>
                <Button
                  variant="destructive"
                  className="flex-1"
                  disabled={!rejectReason}
                >
                  Xác nhận từ chối
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}


export default OwnerApprovalPage;
