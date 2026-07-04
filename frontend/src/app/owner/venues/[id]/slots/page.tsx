'use client'

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { useSession } from "next-auth/react";
import { Button } from "@/components/ui/button";
import { Loader2, ArrowLeft, Clock, Wrench } from "lucide-react";
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse } from "@/types/stadium";
import { toast } from "sonner";
import { TimeSlotManager } from "@/components/venues/TimeSlotManager";
import { MaintenanceScheduleDialog } from "@/components/venues/MaintenanceScheduleDialog";

export default function VenueSlotsPage() {
  const router = useRouter();
  const params = useParams();
  const stadiumId = parseInt(params.id as string);
  const { data: session, status } = useSession();
  const [isLoading, setIsLoading] = useState(true);
  const [stadium, setStadium] = useState<StadiumResponse | null>(null);
  const [isMaintenanceOpen, setIsMaintenanceOpen] = useState(false);
  // Bump để force remount TimeSlotManager (tự load lại weekly slots) sau khi bảo trì thay đổi.
  const [maintenanceRefreshKey, setMaintenanceRefreshKey] = useState(0);

  const fetchStadium = () => {
    return stadiumService.getStadiumById(stadiumId)
      .then((data) => {
        setStadium(data);
      })
      .catch((err) => {
        toast.error(err.message || "Không thể tải thông tin sân");
        router.push("/owner/venues");
      });
  };

  useEffect(() => {
    if (status === "authenticated") {
      fetchStadium().finally(() => setIsLoading(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, stadiumId, router]);

  const handleMaintenanceSuccess = () => {
    fetchStadium();
    setMaintenanceRefreshKey((k) => k + 1);
  };

  if (status === "loading" || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!session || !stadium) return null;

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-5xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-8 gap-4">
          <div className="flex items-center gap-4">
            <Button
              variant="outline"
              size="icon"
              onClick={() => router.push("/owner/venues")}
              className="rounded-full"
            >
              <ArrowLeft className="h-5 w-5" />
            </Button>
            <div>
              <h1 className="text-3xl font-bold">{stadium.stadiumName}</h1>
              <p className="text-muted-foreground flex items-center gap-1.5 mt-1">
                <Clock className="w-4 h-4" />
                Cấu hình khung giờ hoạt động
              </p>
            </div>
          </div>
          <Button variant="outline" onClick={() => setIsMaintenanceOpen(true)}>
            <Wrench className="mr-2 h-4 w-4" />
            Đặt bảo trì
          </Button>
        </div>

        <TimeSlotManager
          key={maintenanceRefreshKey}
          stadiumId={stadiumId}
          openTime={stadium.openTime}
          closeTime={stadium.closeTime}
        />

        <MaintenanceScheduleDialog
          isOpen={isMaintenanceOpen}
          onClose={() => setIsMaintenanceOpen(false)}
          targetType="stadium"
          targetId={stadiumId}
          targetName={stadium.stadiumName}
          onSuccess={handleMaintenanceSuccess}
        />
      </div>
    </div>
  );
}
