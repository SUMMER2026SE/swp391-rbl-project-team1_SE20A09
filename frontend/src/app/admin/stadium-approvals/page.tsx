'use client'

import { useEffect, useState } from "react";
import { Header } from "@/components/layout/Header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CheckCircle, XCircle, MapPin, Building, Loader2, ImageOff } from "lucide-react";
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse } from "@/types/stadium";
import { toast } from "sonner";
import Link from "next/link";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";

export default function StadiumApprovalPage() {
  const router = useRouter();
  const { data: session, status: sessionStatus } = useSession();
  const [stadiums, setStadiums] = useState<StadiumResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  // Authentication guard
  useEffect(() => {
    if (sessionStatus === "unauthenticated") {
      router.replace("/login");
    } else if (sessionStatus === "authenticated" && session?.user?.roleName !== "Admin") {
      toast.error("Bạn không có quyền truy cập trang này");
      router.replace("/");
    }
  }, [sessionStatus, session, router]);

  const fetchStadiums = () => {
    setLoading(true);
    stadiumService.getAllStadiums()
      .then(setStadiums)
      .catch(() => toast.error("Không thể tải danh sách sân chờ duyệt"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (sessionStatus === "authenticated" && session?.user?.roleName === "Admin") {
      fetchStadiums();
    }
  }, [sessionStatus, session]);

  const handleApprove = async (stadiumId: number) => {
    setActionLoadingId(stadiumId);
    try {
      await stadiumService.approveStadium(stadiumId);
      toast.success("Đã duyệt sân thành công");
      fetchStadiums();
    } catch {
      toast.error("Không thể phê duyệt sân");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReject = async (stadiumId: number) => {
    setActionLoadingId(stadiumId);
    try {
      await stadiumService.rejectStadium(stadiumId);
      toast.success("Đã từ chối sân thành công");
      fetchStadiums();
    } catch {
      toast.error("Không thể từ chối sân");
    } finally {
      setActionLoadingId(null);
    }
  };

  const pendingStadiums = stadiums.filter(s => s.approvedStatus === "PENDING");
  const approvedStadiums = stadiums.filter(s => s.approvedStatus === "APPROVED");
  const rejectedStadiums = stadiums.filter(s => s.approvedStatus === "REJECTED");

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "AVAILABLE":
        return <Badge className="bg-green-100 text-green-700 hover:bg-green-100">Hoạt động</Badge>;
      case "MAINTENANCE":
        return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">Bảo trì</Badge>;
      case "CLOSED":
        return <Badge className="bg-red-100 text-red-700 hover:bg-red-100">Đóng cửa</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const StadiumRow = ({ stadium }: { stadium: StadiumResponse }) => (
    <Card className="mb-4 overflow-hidden border-slate-200 hover:shadow-sm transition-shadow">
      <CardContent className="p-6">
        <div className="flex flex-col md:flex-row gap-6">
          {/* Main Image preview */}
          <div className="w-full md:w-48 h-32 relative bg-muted rounded-lg overflow-hidden shrink-0">
            {stadium.imageUrls && stadium.imageUrls.length > 0 ? (
              <img
                src={stadium.imageUrls[0]}
                alt={stadium.stadiumName}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-muted-foreground">
                <ImageOff className="h-8 w-8" />
              </div>
            )}
          </div>

          {/* Details */}
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <h3 className="text-xl font-bold truncate">{stadium.stadiumName}</h3>
              <Badge variant="outline" className="font-normal">{stadium.sportName}</Badge>
              {getStatusBadge(stadium.stadiumStatus)}
            </div>

            <div className="space-y-1.5 text-sm text-muted-foreground mb-4">
              <p className="flex items-start gap-1">
                <MapPin className="h-4 w-4 mt-0.5 shrink-0" />
                <span>{stadium.address}</span>
              </p>
              <p className="flex items-center gap-1">
                <Building className="h-4 w-4 shrink-0" />
                <span>Giá thuê: {stadium.pricePerHour.toLocaleString('vi-VN')}đ/giờ • Sức chứa: {stadium.capacity} người</span>
              </p>
              {stadium.description && (
                <p className="text-xs line-clamp-2 italic">Mô tả: {stadium.description}</p>
              )}
            </div>

            {/* Actions */}
            {stadium.approvedStatus === "PENDING" && (
              <div className="flex gap-3 max-w-sm">
                <Button
                  size="sm"
                  className="flex-1"
                  disabled={actionLoadingId !== null}
                  onClick={() => handleApprove(stadium.stadiumId)}
                >
                  {actionLoadingId === stadium.stadiumId ? (
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  ) : (
                    <CheckCircle className="h-4 w-4 mr-2" />
                  )}
                  Phê duyệt
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  className="flex-1"
                  disabled={actionLoadingId !== null}
                  onClick={() => handleReject(stadium.stadiumId)}
                >
                  {actionLoadingId === stadium.stadiumId ? (
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  ) : (
                    <XCircle className="h-4 w-4 mr-2" />
                  )}
                  Từ chối
                </Button>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );

  if (sessionStatus === "loading" || loading) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex justify-center items-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold">Duyệt sân thể thao</h1>
            <p className="text-muted-foreground mt-1">Review and approve new sport venues or changes.</p>
          </div>
          <Button variant="outline" onClick={() => router.push("/admin/dashboard")}>
            Quay lại Dashboard
          </Button>
        </div>

        <Tabs defaultValue="pending">
          <TabsList className="mb-6">
            <TabsTrigger value="pending">
              Chờ duyệt ({pendingStadiums.length})
            </TabsTrigger>
            <TabsTrigger value="approved">
              Đã duyệt ({approvedStadiums.length})
            </TabsTrigger>
            <TabsTrigger value="rejected">
              Từ chối ({rejectedStadiums.length})
            </TabsTrigger>
          </TabsList>

          <TabsContent value="pending">
            {pendingStadiums.length === 0 ? (
              <div className="text-center py-20 text-muted-foreground border-2 border-dashed rounded-lg bg-card">
                Hiện tại không có sân nào đang chờ duyệt.
              </div>
            ) : (
              pendingStadiums.map((stadium) => (
                <StadiumRow key={stadium.stadiumId} stadium={stadium} />
              ))
            )}
          </TabsContent>

          <TabsContent value="approved">
            {approvedStadiums.length === 0 ? (
              <div className="text-center py-20 text-muted-foreground border-2 border-dashed rounded-lg bg-card">
                Chưa có sân nào được phê duyệt.
              </div>
            ) : (
              approvedStadiums.map((stadium) => (
                <StadiumRow key={stadium.stadiumId} stadium={stadium} />
              ))
            )}
          </TabsContent>

          <TabsContent value="rejected">
            {rejectedStadiums.length === 0 ? (
              <div className="text-center py-20 text-muted-foreground border-2 border-dashed rounded-lg bg-card">
                Chưa có sân nào bị từ chối.
              </div>
            ) : (
              rejectedStadiums.map((stadium) => (
                <StadiumRow key={stadium.stadiumId} stadium={stadium} />
              ))
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
