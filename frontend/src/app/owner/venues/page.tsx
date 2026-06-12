'use client'

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Plus, MoreVertical, Edit, Settings, Eye, Pause, PlayCircle, Trash, Loader2, ImageOff, Package } from "lucide-react";
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse } from "@/types/stadium";
import { toast } from "sonner";
import { AccessoryManagerDialog } from "@/components/venues/AccessoryManagerDialog";

function VenueManagementPage() {
  const router = useRouter();
  const [venues, setVenues] = useState<StadiumResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAccessoryOpen, setIsAccessoryOpen] = useState(false);
  const [selectedVenueForAccessory, setSelectedVenueForAccessory] = useState<{ id: number, name: string } | null>(null);
  
  const [suspendVenue, setSuspendVenue] = useState<{ id: number, name: string } | null>(null);
  const [activateVenue, setActivateVenue] = useState<{ id: number, name: string } | null>(null);
  const [deleteVenue, setDeleteVenue] = useState<{ id: number, name: string } | null>(null);
  const [deleteConfirmText, setDeleteConfirmText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    stadiumService.getMyStadiums()
      .then(setVenues)
      .catch(() => toast.error("Không thể tải danh sách sân"))
      .finally(() => setLoading(false));
  }, []);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "AVAILABLE":
        return <Badge className="bg-green-100 text-green-700">Hoạt động</Badge>;
      case "MAINTENANCE":
        return <Badge className="bg-yellow-100 text-yellow-700">Bảo trì</Badge>;
      case "CLOSED":
        return <Badge className="bg-red-100 text-red-700">Đóng cửa</Badge>;
      default:
        return <Badge className="bg-gray-100 text-gray-700">{status}</Badge>;
    }
  };

  const handleSuspend = async () => {
    if (!suspendVenue) return;
    setIsSubmitting(true);
    try {
      await stadiumService.suspendStadium(suspendVenue.id);
      toast.success("Đã tạm ngưng hoạt động sân thành công.");
      setVenues(venues.map(v => v.stadiumId === suspendVenue.id ? { ...v, stadiumStatus: "MAINTENANCE" } : v));
      setSuspendVenue(null);
    } catch (error) {
      toast.error("Có lỗi xảy ra khi tạm ngưng sân.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleActivate = async () => {
    if (!activateVenue) return;
    setIsSubmitting(true);
    try {
      await stadiumService.activateStadium(activateVenue.id);
      toast.success("Đã mở lại hoạt động sân thành công.");
      setVenues(venues.map(v => v.stadiumId === activateVenue.id ? { ...v, stadiumStatus: "AVAILABLE" } : v));
      setActivateVenue(null);
    } catch (error) {
      toast.error("Có lỗi xảy ra khi mở lại sân.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteVenue) return;
    if (deleteConfirmText !== "DELETE") {
      toast.error("Vui lòng nhập chữ DELETE để xác nhận.");
      return;
    }
    setIsSubmitting(true);
    try {
      await stadiumService.deleteStadium(deleteVenue.id);
      toast.success("Đã xóa sân thành công.");
      setVenues(venues.map(v => v.stadiumId === deleteVenue.id ? { ...v, stadiumStatus: "CLOSED" } : v));
      setDeleteVenue(null);
      setDeleteConfirmText("");
    } catch (error) {
      toast.error("Có lỗi xảy ra khi xóa sân.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Quản lý sân</h1>
        <Button onClick={() => router.push("/owner/venues/new")}>
          <Plus className="mr-2 h-5 w-5" />
          Thêm sân mới
        </Button>
      </div>

      {loading ? (
        <div className="flex justify-center items-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : venues.length === 0 ? (
        <div className="text-center py-20 text-muted-foreground">
          <p className="text-lg mb-4">Bạn chưa có sân nào.</p>
          <Button onClick={() => router.push("/owner/venues/new")}>
            <Plus className="mr-2 h-5 w-5" />
            Thêm sân đầu tiên
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {venues.map((venue) => (
            <Card key={venue.stadiumId} className="overflow-hidden">
              <div className="relative h-48 bg-muted">
                {venue.imageUrls && venue.imageUrls.length > 0 ? (
                  <img
                    src={venue.imageUrls[0]}
                    alt={venue.stadiumName}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-muted-foreground">
                    <ImageOff className="h-12 w-12" />
                  </div>
                )}
                <div className="absolute top-3 right-3">
                  {getStatusBadge(venue.stadiumStatus)}
                </div>
              </div>

              <CardContent className="p-6">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold mb-2 truncate">{venue.stadiumName}</h3>
                    <Badge variant="outline">{venue.sportName}</Badge>
                  </div>

                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="sm">
                        <MoreVertical className="h-5 w-5" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem>
                        <Edit className="mr-2 h-4 w-4" />
                        Chỉnh sửa
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Settings className="mr-2 h-4 w-4" />
                        Cấu hình lịch
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Eye className="mr-2 h-4 w-4" />
                        Xem đặt sân
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => {
                        setSelectedVenueForAccessory({ id: venue.stadiumId, name: venue.stadiumName });
                        setIsAccessoryOpen(true);
                      }}>
                        <Package className="mr-2 h-4 w-4" />
                        Quản lý phụ kiện
                      </DropdownMenuItem>
                      {venue.stadiumStatus === 'AVAILABLE' ? (
                        <DropdownMenuItem onClick={() => setSuspendVenue({ id: venue.stadiumId, name: venue.stadiumName })}>
                          <Pause className="mr-2 h-4 w-4" />
                          Tạm dừng
                        </DropdownMenuItem>
                      ) : venue.stadiumStatus === 'MAINTENANCE' ? (
                        <DropdownMenuItem onClick={() => setActivateVenue({ id: venue.stadiumId, name: venue.stadiumName })}>
                          <PlayCircle className="mr-2 h-4 w-4" />
                          Mở lại
                        </DropdownMenuItem>
                      ) : null}
                      {venue.stadiumStatus !== 'CLOSED' && (
                        <DropdownMenuItem className="text-destructive" onClick={() => setDeleteVenue({ id: venue.stadiumId, name: venue.stadiumName })}>
                          <Trash className="mr-2 h-4 w-4" />
                          Xóa
                        </DropdownMenuItem>
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>

                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <p className="truncate">{venue.address}</p>
                  <p className="text-primary font-medium">
                    {venue.pricePerHour.toLocaleString('vi-VN')}đ / giờ
                  </p>
                </div>

                <div className="flex gap-2 mt-4">
                  <Button variant="outline" size="sm" className="flex-1">
                    Chỉnh sửa
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 text-primary hover:text-primary hover:bg-primary/5 border-primary/20"
                    onClick={() => {
                      setSelectedVenueForAccessory({ id: venue.stadiumId, name: venue.stadiumName });
                      setIsAccessoryOpen(true);
                    }}
                  >
                    <Package className="mr-1 h-4 w-4" />
                    Phụ kiện
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {selectedVenueForAccessory && (
        <AccessoryManagerDialog
          stadiumId={selectedVenueForAccessory.id}
          stadiumName={selectedVenueForAccessory.name}
          isOpen={isAccessoryOpen}
          onClose={() => {
            setIsAccessoryOpen(false);
            setSelectedVenueForAccessory(null);
          }}
        />
      )}

      {/* Suspend Modal */}
      <Dialog open={!!suspendVenue} onOpenChange={(open) => !open && setSuspendVenue(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác nhận tạm dừng sân</DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn tạm dừng hoạt động sân <strong>{suspendVenue?.name}</strong> không? 
              Trạng thái sân sẽ chuyển sang bảo trì và khách hàng không thể đặt sân mới.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setSuspendVenue(null)} disabled={isSubmitting}>Hủy</Button>
            <Button onClick={handleSuspend} disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Xác nhận
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Activate Modal */}
      <Dialog open={!!activateVenue} onOpenChange={(open) => !open && setActivateVenue(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác nhận mở lại sân</DialogTitle>
            <DialogDescription>
              Sân <strong>{activateVenue?.name}</strong> sẽ được chuyển sang trạng thái hoạt động và khách hàng có thể tiếp tục đặt sân.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setActivateVenue(null)} disabled={isSubmitting}>Hủy</Button>
            <Button onClick={handleActivate} disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Xác nhận
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Modal */}
      <Dialog open={!!deleteVenue} onOpenChange={(open) => !open && setDeleteVenue(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xóa sân (Nguy hiểm)</DialogTitle>
            <DialogDescription className="text-destructive">
              Hành động này sẽ đóng cửa sân <strong>{deleteVenue?.name}</strong> vĩnh viễn và hủy tất cả các lịch đặt chưa diễn ra. Hành động này không thể hoàn tác!
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <p className="text-sm mb-2">Vui lòng nhập <strong>DELETE</strong> để xác nhận:</p>
            <Input 
              value={deleteConfirmText}
              onChange={(e) => setDeleteConfirmText(e.target.value)}
              placeholder="DELETE"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => { setDeleteVenue(null); setDeleteConfirmText(""); }} disabled={isSubmitting}>Hủy</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={isSubmitting || deleteConfirmText !== "DELETE"}>
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Xóa sân
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
}

export default VenueManagementPage;
