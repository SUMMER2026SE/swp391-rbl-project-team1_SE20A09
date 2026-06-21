'use client'

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CheckCircle, XCircle, MapPin, Building, Loader2, ImageOff } from "lucide-react";
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse } from "@/types/stadium";
import { toast } from "sonner";
import Link from "next/link";

import Image from "next/image";

export default function StadiumApprovalPage() {
  const [stadiums, setStadiums] = useState<StadiumResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState("PENDING");

  const fetchStadiums = (status: string) => {
    setLoading(true);
    stadiumService.getAllStadiums(status)
      .then(setStadiums)
      .catch(() => toast.error("Không thể tải danh sách sân chờ duyệt"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchStadiums(activeTab);
  }, [activeTab]);

  const handleApprove = async (stadiumId: number) => {
    setActionLoadingId(stadiumId);
    try {
      await stadiumService.approveStadium(stadiumId);
      toast.success("Đã duyệt sân thành công");
      fetchStadiums(activeTab);
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
      fetchStadiums(activeTab);
    } catch {
      toast.error("Không thể từ chối sân");
    } finally {
      setActionLoadingId(null);
    }
  };

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
              <Image
                src={stadium.imageUrls[0]}
                alt={stadium.stadiumName}
                fill
                className="object-cover"
                unoptimized
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
                <span>Giá thuê: {stadium.pricePerHour.toLocaleString('vi-VN')}đ/giờ</span>
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

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold">Duyệt sân thể thao</h1>
            <p className="text-muted-foreground mt-1">Review and approve new sport venues or changes.</p>
          </div>
        </div>

        <Tabs value={activeTab.toLowerCase()} onValueChange={(val) => setActiveTab(val.toUpperCase())}>
          <TabsList className="mb-6">
            <TabsTrigger value="pending">
              Chờ duyệt
            </TabsTrigger>
            <TabsTrigger value="approved">
              Đã duyệt
            </TabsTrigger>
            <TabsTrigger value="rejected">
              Từ chối
            </TabsTrigger>
          </TabsList>

          <TabsContent value="pending">
            {stadiums.length === 0 ? (
              <div className="text-center py-20 text-muted-foreground border-2 border-dashed rounded-lg bg-card">
                Hiện tại không có sân nào đang chờ duyệt.
              </div>
            ) : (
              stadiums.map((stadium) => (
                <StadiumRow key={stadium.stadiumId} stadium={stadium} />
              ))
            )}
          </TabsContent>

          <TabsContent value="approved">
            {stadiums.length === 0 ? (
              <div className="text-center py-20 text-muted-foreground border-2 border-dashed rounded-lg bg-card">
                Chưa có sân nào được phê duyệt.
              </div>
            ) : (
              stadiums.map((stadium) => (
                <StadiumRow key={stadium.stadiumId} stadium={stadium} />
              ))
            )}
          </TabsContent>

          <TabsContent value="rejected">
            {stadiums.length === 0 ? (
              <div className="text-center py-20 text-muted-foreground border-2 border-dashed rounded-lg bg-card">
                Chưa có sân nào bị từ chối.
              </div>
            ) : (
              stadiums.map((stadium) => (
                <StadiumRow key={stadium.stadiumId} stadium={stadium} />
              ))
            )}
          </TabsContent>
        </Tabs>
    </div>
  );
}
