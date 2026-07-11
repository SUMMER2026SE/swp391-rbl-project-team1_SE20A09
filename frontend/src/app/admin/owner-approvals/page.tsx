'use client'

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
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
import { CheckCircle, XCircle, MapPin, Building, Loader2, Mail, Phone, Calendar } from "lucide-react";
import { adminOwnerService, OwnerDetail } from "@/lib/services/admin-owner";
import { toast } from "sonner";
import { useSession } from "next-auth/react";
import { usePathname, useRouter } from "next/navigation";

function OwnerApprovalContent() {
  const router = useRouter();
  const { data: session, status: sessionStatus } = useSession();

  const fileToken = session?.accessToken ? `?token=${session.accessToken}` : "";
  const getFileUrl = (fileName?: string) => {
    if (!fileName) return "#";
    if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
      const separator = fileName.includes("?") ? "&" : "?";
      return `${fileName}${session?.accessToken ? `${separator}token=${session.accessToken}` : ""}`;
    }
    return `${process.env.NEXT_PUBLIC_API_URL}/files/documents/${fileName}${fileToken}`;
  };

  const [activeTab, setActiveTab] = useState<'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING');
  const [applications, setApplications] = useState<OwnerDetail[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  
  const [selectedApplication, setSelectedApplication] = useState<OwnerDetail | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  useEffect(() => {
    if (sessionStatus === "authenticated" && session?.user?.roleName !== "Admin") {
      toast.error("Bạn không có quyền truy cập trang này");
      router.replace("/");
    }
  }, [sessionStatus, session, router]);

  const fetchApplications = async (status: 'PENDING' | 'APPROVED' | 'REJECTED', pageNum: number) => {
    setLoading(true);
    try {
      const res = await adminOwnerService.getRegistrations(status, pageNum, 10);
      if (res.code === 200 && res.result) {
        setApplications(res.result.content);
        setTotalPages(res.result.totalPages);
      } else {
        toast.error(res.message || "Không thể tải danh sách hồ sơ");
      }
    } catch (error: any) {
      toast.error(error.message || "Lỗi khi tải danh sách hồ sơ");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (sessionStatus === "authenticated" && session?.user?.roleName === "Admin") {
      fetchApplications(activeTab, page);
    }
  }, [sessionStatus, session, activeTab, page]);

  const handleTabChange = (val: string) => {
    setActiveTab(val as 'PENDING' | 'APPROVED' | 'REJECTED');
    setPage(0); // reset to page 0 when tab changes
  };

  const handleApprove = async (ownerId: number) => {
    setActionLoadingId(ownerId);
    try {
      const res = await adminOwnerService.approveOrReject(ownerId, { approvedStatus: 'APPROVED' });
      if (res.code === 200) {
        toast.success("Đã phê duyệt hồ sơ đối tác thành công");
        fetchApplications(activeTab, page);
      } else {
        toast.error(res.message || "Không thể phê duyệt hồ sơ");
      }
    } catch (error: any) {
      toast.error(error.message || "Có lỗi xảy ra khi phê duyệt");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleRejectClick = (app: OwnerDetail) => {
    setSelectedApplication(app);
    setRejectReason("");
  };

  const handleConfirmReject = async () => {
    if (!selectedApplication) return;
    if (!rejectReason.trim()) {
      toast.error("Vui lòng nhập lý do từ chối");
      return;
    }

    setActionLoadingId(selectedApplication.ownerId);
    try {
      const res = await adminOwnerService.approveOrReject(selectedApplication.ownerId, {
        approvedStatus: 'REJECTED',
        rejectionReason: rejectReason.trim()
      });
      if (res.code === 200) {
        toast.success("Đã từ chối đơn đăng ký thành công");
        setSelectedApplication(null);
        fetchApplications(activeTab, page);
      } else {
        toast.error(res.message || "Không thể từ chối hồ sơ");
      }
    } catch (error: any) {
      toast.error(error.message || "Có lỗi xảy ra khi từ chối");
    } finally {
      setActionLoadingId(null);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PENDING":
        return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">Chờ duyệt</Badge>;
      case "APPROVED":
        return <Badge className="bg-green-100 text-green-700 hover:bg-green-100">Đã duyệt</Badge>;
      case "REJECTED":
        return <Badge className="bg-red-100 text-red-700 hover:bg-red-100">Đã từ chối</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const ApplicationCard = ({ application }: { application: OwnerDetail }) => (
    <Card className="mb-4 border-slate-200 hover:shadow-sm transition-shadow">
      <CardContent className="p-6">
        <div className="flex flex-col md:flex-row items-start justify-between gap-4 mb-4">
          <div className="flex items-start gap-4">
            <Avatar className="h-12 w-12">
              <AvatarImage src={`https://api.dicebear.com/7.x/adventurer/svg?seed=${application.email}`} />
              <AvatarFallback>{application.fullName[0]}</AvatarFallback>
            </Avatar>
            <div>
              <h3 className="text-lg font-bold mb-1">{application.fullName}</h3>
              <div className="flex flex-col gap-1 text-sm text-muted-foreground mb-2">
                <span className="flex items-center gap-1.5">
                  <Mail className="h-3.5 w-3.5" /> {application.email}
                </span>
                <span className="flex items-center gap-1.5">
                  <Phone className="h-3.5 w-3.5" /> {application.phoneNumber}
                </span>
              </div>
              {getStatusBadge(application.approvedStatus)}
            </div>
          </div>
          <div className="flex items-center gap-1.5 text-sm text-muted-foreground self-end md:self-start">
            <Calendar className="h-4 w-4" />
            <span>Ngày nộp: {new Date(application.createdAt).toLocaleDateString('vi-VN')}</span>
          </div>
        </div>

        <div className="bg-muted/50 rounded-lg p-4 mb-4">
          <div className="flex items-center gap-2 mb-3">
            <Building className="h-5 w-5 text-primary" />
            <h4 className="font-semibold text-slate-800">Thông tin doanh nghiệp / hộ kinh doanh</h4>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm mb-4">
            <div>
              <span className="text-muted-foreground block md:inline font-medium">Tên thương hiệu:</span>{" "}
              <span className="font-semibold text-slate-700">{application.businessName}</span>
            </div>
            <div>
              <span className="text-muted-foreground block md:inline font-medium">Mã số thuế:</span>{" "}
              <span className="font-semibold text-slate-700">{application.taxCode}</span>
            </div>
            <div className="md:col-span-2 flex items-start gap-1.5">
              <MapPin className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
              <span>
                <span className="text-muted-foreground font-medium">Địa chỉ kinh doanh:</span>{" "}
                {application.businessAddress}
              </span>
            </div>
          </div>

          <div className="mt-4 pt-4 border-t border-slate-200">
            <span className="text-muted-foreground font-semibold text-xs block mb-2">Tài liệu đính kèm:</span>
            <div className="flex flex-wrap gap-3">
              {application.businessLicenseUrl ? (
                <a
                  href={getFileUrl(application.businessLicenseUrl)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 px-3 py-1.5 rounded-md bg-white border border-slate-200 hover:border-primary text-xs font-semibold text-slate-700 hover:text-primary transition-all shadow-sm"
                >
                  📄 Giấy phép kinh doanh
                </a>
              ) : (
                <span className="text-xs text-red-500 font-semibold">⚠️ Thiếu Giấy phép kinh doanh</span>
              )}
              {application.identityCardUrl ? (
                <a
                  href={getFileUrl(application.identityCardUrl)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 px-3 py-1.5 rounded-md bg-white border border-slate-200 hover:border-primary text-xs font-semibold text-slate-700 hover:text-primary transition-all shadow-sm"
                >
                  🪪 Ảnh CCCD / Hộ chiếu
                </a>
              ) : (
                <span className="text-xs text-red-500 font-semibold">⚠️ Thiếu ảnh CCCD</span>
              )}
            </div>
          </div>
        </div>

        {application.approvedStatus === "PENDING" && (
          <div className="flex justify-end gap-3 mt-4">
            <Button
              variant="destructive"
              className="px-4 py-2"
              disabled={actionLoadingId !== null}
              onClick={() => handleRejectClick(application)}
            >
              <XCircle className="h-4 w-4 mr-2" />
              Từ chối
            </Button>
            <Button
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white"
              disabled={actionLoadingId !== null}
              onClick={() => handleApprove(application.ownerId)}
            >
              {actionLoadingId === application.ownerId ? (
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
              ) : (
                <CheckCircle className="h-4 w-4 mr-2" />
              )}
              Phê duyệt
            </Button>
          </div>
        )}

        {application.approvedStatus === "REJECTED" && application.rejectionReason && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            <strong>Lý do từ chối:</strong> {application.rejectionReason}
          </div>
        )}
      </CardContent>
    </Card>
  );

  if (sessionStatus === "loading") {
    return (
      <div className="flex justify-center items-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Tabs value={activeTab} onValueChange={handleTabChange}>
          <TabsList className="mb-6">
            <TabsTrigger value="PENDING">Chờ duyệt</TabsTrigger>
            <TabsTrigger value="APPROVED">Đã duyệt</TabsTrigger>
            <TabsTrigger value="REJECTED">Từ chối</TabsTrigger>
          </TabsList>

          <TabsContent value={activeTab}>
            {loading ? (
              <div className="flex flex-col items-center justify-center py-20 gap-3">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                <span className="text-sm text-muted-foreground">Đang tải danh sách hồ sơ...</span>
              </div>
            ) : applications.length === 0 ? (
              <div className="text-center py-20 text-muted-foreground border-2 border-dashed rounded-lg bg-card">
                {activeTab === "PENDING" && "Hiện tại không có hồ sơ nào đang chờ duyệt."}
                {activeTab === "APPROVED" && "Chưa có hồ sơ đối tác nào được duyệt."}
                {activeTab === "REJECTED" && "Chưa có hồ sơ đối tác nào bị từ chối."}
              </div>
            ) : (
              <div>
                {applications.map((app) => (
                  <ApplicationCard key={app.ownerId} application={app} />
                ))}

                {totalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 mt-6">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page === 0 || loading}
                      onClick={() => setPage(prev => prev - 1)}
                    >
                      Trang trước
                    </Button>
                    <span className="text-sm text-muted-foreground">
                      Trang {page + 1} / {totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page >= totalPages - 1 || loading}
                      onClick={() => setPage(prev => prev + 1)}
                    >
                      Trang sau
                    </Button>
                  </div>
                )}
              </div>
            )}
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
                <p className="text-sm text-muted-foreground mb-3">
                  Từ chối hồ sơ đăng ký của <strong>{selectedApplication?.fullName}</strong> ({selectedApplication?.businessName}).
                </p>
                <label className="block text-sm font-medium mb-2">Lý do từ chối *</label>
                <Textarea
                  placeholder="Nhập lý do từ chối..."
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  className="focus-visible:ring-rose-500"
                  rows={4}
                />
                <p className="text-xs text-muted-foreground mt-1">
                  Lý do này sẽ được gửi trực tiếp qua Email để Chủ sân cập nhật lại hồ sơ.
                </p>
                <div className="mt-3">
                  <span className="text-xs font-semibold text-slate-500 block mb-1.5">Gợi ý lý do nhanh:</span>
                  <div className="flex flex-wrap gap-1.5">
                    {[
                      "Ảnh tài liệu bị mờ, không rõ thông tin",
                      "Mã số thuế không hợp lệ hoặc không trùng khớp",
                      "Địa chỉ kinh doanh thiếu chi tiết",
                      "Tài liệu giấy tờ tùy thân không chính xác"
                    ].map((tag) => (
                      <Badge
                        key={tag}
                        variant="outline"
                        className="cursor-pointer hover:bg-slate-100 hover:text-slate-900 transition-colors text-[11px] py-1 px-2.5 font-normal"
                        onClick={() => setRejectReason(tag)}
                      >
                        {tag}
                      </Badge>
                    ))}
                  </div>
                </div>
              </div>
              <div className="flex gap-2 justify-end">
                <Button
                  variant="outline"
                  onClick={() => setSelectedApplication(null)}
                  disabled={actionLoadingId !== null}
                >
                  Hủy
                </Button>
                <Button
                  variant="destructive"
                  disabled={!rejectReason.trim() || actionLoadingId !== null}
                  onClick={handleConfirmReject}
                >
                  {actionLoadingId !== null && <Loader2 className="h-4 w-4 animate-spin mr-2" />}
                  Xác nhận từ chối
                </Button>
              </div>
            </div>
          </DialogContent>
      </Dialog>
    </div>
  );
}

export default function OwnerApprovalPage() {
  const router = useRouter();
  const pathname = usePathname();

  if (pathname.startsWith("/admin/users")) {
    return <OwnerApprovalContent />;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold">Duyệt hồ sơ chủ sân</h1>
          <p className="text-muted-foreground mt-1">Xem xét hồ sơ đăng ký kinh doanh và yêu cầu nâng cấp lên Chủ sân.</p>
        </div>
        <Button variant="outline" onClick={() => router.push("/admin/dashboard")}>
          Quay lại Dashboard
        </Button>
      </div>
      <OwnerApprovalContent />
    </div>
  );
}
