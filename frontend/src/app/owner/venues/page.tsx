'use client'

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Plus,
  MoreVertical,
  Edit,
  Pause,
  PlayCircle,
  Trash,
  Loader2,
  ImageOff,
  Package,
  CalendarDays,
  LayoutGrid,
  List as ListIcon,
  Search,
} from "lucide-react";
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse, SportType } from "@/types/stadium";
import { toast } from "sonner";
import { AccessoryManagerDialog } from "@/components/venues/AccessoryManagerDialog";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { useConfirm } from "@/hooks/useConfirm";

interface VenueModalData {
  id: number;
  name: string;
}

function VenueManagementPage() {
  const router = useRouter();
  const [venues, setVenues] = useState<StadiumResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAccessoryOpen, setIsAccessoryOpen] = useState(false);
  const [selectedVenueForAccessory, setSelectedVenueForAccessory] = useState<VenueModalData | null>(null);
  
  const [suspendVenue, setSuspendVenue] = useState<VenueModalData | null>(null);
  const [activateVenue, setActivateVenue] = useState<VenueModalData | null>(null);
  const [deleteVenue, setDeleteVenue] = useState<VenueModalData | null>(null);
  const [deleteConfirmText, setDeleteConfirmText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Filter and view states
  const [search, setSearch] = useState("");
  const [sportTypeId, setSportTypeId] = useState<number | null>(null);
  const [status, setStatus] = useState<string>("all");
  const [viewMode, setViewMode] = useState<"grid" | "table">("grid");
  const [sportTypes, setSportTypes] = useState<SportType[]>([]);

  // Custom confirm hook
  const { 
    isOpen: isConfirmOpen, 
    isLoading: isActionLoading, 
    options: confirmOptions, 
    close: closeConfirm, 
    execute: executeConfirm 
  } = useConfirm()

  useEffect(() => {
    stadiumService.getSportTypes()
      .then(setSportTypes)
      .catch(() => toast.error("Không thể tải danh sách môn thể thao"));
  }, []);

  const fetchVenues = useCallback(() => {
    setLoading(true);
    stadiumService.getMyStadiums({
      search: search || undefined,
      sportTypeId: sportTypeId || undefined,
      status: status === "all" ? undefined : status
    })
      .then(setVenues)
      .catch(() => toast.error("Không thể tải danh sách sân"))
      .finally(() => setLoading(false));
  }, [search, sportTypeId, status]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchVenues();
    }, 300);
    return () => clearTimeout(timer);
  }, [fetchVenues]);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "AVAILABLE":
        return <Badge className="bg-green-100 text-green-700 hover:bg-green-100">Hoạt động</Badge>;
      case "MAINTENANCE":
        return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">Bảo trì</Badge>;
      case "CLOSED":
        return <Badge className="bg-red-100 text-red-700 hover:bg-red-100">Đóng cửa</Badge>;
      default:
        return <Badge className="bg-gray-100 text-gray-700 hover:bg-gray-100">{status}</Badge>;
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
      setVenues(venues.filter(v => v.stadiumId !== deleteVenue.id));
      setDeleteVenue(null);
      setDeleteConfirmText("");
    } catch (error) {
      toast.error("Có lỗi xảy ra khi xóa sân.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const getApprovedStatusBadge = (approvedStatus: string) => {
    switch (approvedStatus) {
      case "PENDING":
        return <Badge className="bg-amber-100 text-amber-700 border-amber-200 hover:bg-amber-100">Chờ duyệt</Badge>;
      case "APPROVED":
        return <Badge className="bg-emerald-100 text-emerald-700 border-emerald-200 hover:bg-emerald-100">Đã duyệt</Badge>;
      case "REJECTED":
        return <Badge className="bg-rose-100 text-rose-700 border-rose-200 hover:bg-rose-100">Từ chối</Badge>;
      default:
        return <Badge variant="outline">{approvedStatus}</Badge>;
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

      {/* Filter and View Toggle Row */}
      <div className="flex flex-col md:flex-row gap-4 mb-6 items-center justify-between bg-slate-50/50 p-4 rounded-xl border border-slate-100">
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          {/* Search name */}
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Tìm tên sân..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 bg-white"
            />
          </div>

          {/* Sport Categories */}
          <Select
            value={sportTypeId?.toString() || "all"}
            onValueChange={(val) => setSportTypeId(val === "all" ? null : parseInt(val))}
          >
            <SelectTrigger className="w-full sm:w-48 bg-white">
              <SelectValue placeholder="Môn thể thao" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả môn thể thao</SelectItem>
              {sportTypes.map((t) => (
                <SelectItem key={t.sportTypeId} value={t.sportTypeId.toString()}>
                  {t.sportName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* Stadium Status */}
          <Select
            value={status}
            onValueChange={setStatus}
          >
            <SelectTrigger className="w-full sm:w-44 bg-white">
              <SelectValue placeholder="Trạng thái" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả trạng thái</SelectItem>
              <SelectItem value="AVAILABLE">Đang hoạt động</SelectItem>
              <SelectItem value="MAINTENANCE">Bảo trì</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* View Mode Toggle Buttons */}
        <div className="flex items-center gap-2 border rounded-lg p-1 bg-white self-end md:self-auto shadow-sm">
          <Button
            variant={viewMode === "grid" ? "secondary" : "ghost"}
            size="icon"
            className="h-8 w-8"
            onClick={() => setViewMode("grid")}
            title="Dạng lưới"
          >
            <LayoutGrid className="h-4 w-4" />
          </Button>
          <Button
            variant={viewMode === "table" ? "secondary" : "ghost"}
            size="icon"
            className="h-8 w-8"
            onClick={() => setViewMode("table")}
            title="Dạng bảng"
          >
            <ListIcon className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center items-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : venues.length === 0 ? (
        <div className="text-center py-20 text-muted-foreground bg-card rounded-xl border border-dashed p-6">
          <p className="text-lg mb-4">Không tìm thấy sân nào phù hợp.</p>
          <Button onClick={() => { setSearch(""); setSportTypeId(null); setStatus("all"); }}>
            Xóa bộ lọc
          </Button>
        </div>
      ) : viewMode === "table" ? (
        /* Table View */
        <div className="border rounded-xl bg-card overflow-hidden shadow-sm">
          <Table>
            <TableHeader className="bg-slate-50/50">
              <TableRow>
                <TableHead className="w-16">Ảnh</TableHead>
                <TableHead>Tên sân</TableHead>
                <TableHead>Môn thể thao</TableHead>
                <TableHead className="hidden md:table-cell">Địa chỉ</TableHead>
                <TableHead>Giá thuê/giờ</TableHead>
                <TableHead>Hoạt động</TableHead>
                <TableHead>Phê duyệt</TableHead>
                <TableHead className="w-12 text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {venues.map((venue) => (
                <TableRow key={venue.stadiumId} className="hover:bg-slate-50/50 transition-colors">
                  <TableCell>
                    <div className="w-12 h-8 relative rounded bg-muted overflow-hidden shrink-0 border border-slate-100">
                      {venue.imageUrls && venue.imageUrls.length > 0 ? (
                        <Image
                          src={venue.imageUrls[0]}
                          alt={venue.stadiumName}
                          fill
                          className="object-cover"
                          unoptimized
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-muted-foreground">
                          <ImageOff className="h-4 w-4" />
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="font-semibold truncate max-w-[200px]" title={venue.stadiumName}>
                    {venue.stadiumName}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className="font-normal">{venue.sportName}</Badge>
                  </TableCell>
                  <TableCell className="hidden md:table-cell truncate max-w-[300px]" title={venue.address}>
                    {venue.address}
                  </TableCell>
                  <TableCell className="font-semibold text-slate-900">
                    {venue.pricePerHour.toLocaleString('vi-VN')}đ
                  </TableCell>
                  <TableCell>{getStatusBadge(venue.stadiumStatus)}</TableCell>
                  <TableCell>{getApprovedStatusBadge(venue.approvedStatus)}</TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <MoreVertical className="h-5 w-5" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-48">
                        <DropdownMenuItem onClick={() => router.push(`/owner/venues/${venue.stadiumId}/edit`)}>
                          <Edit className="mr-2 h-4 w-4" />
                          Chỉnh sửa
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => router.push(`/owner/venues/${venue.stadiumId}/slots`)}>
                          <CalendarDays className="mr-2 h-4 w-4" />
                          Cấu hình lịch
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => {
                          setSelectedVenueForAccessory({ id: venue.stadiumId, name: venue.stadiumName });
                          setIsAccessoryOpen(true);
                        }}>
                          <Package className="mr-2 h-4 w-4" />
                          Quản lý phụ kiện
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                          className="text-destructive focus:bg-destructive focus:text-destructive-foreground"
                          onClick={() => setDeleteVenue({ id: venue.stadiumId, name: venue.stadiumName })}
                        >
                          <Trash className="mr-2 h-4 w-4" />
                          Xóa sân
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : (
        /* Grid View */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {venues.map((venue) => (
            <Card key={venue.stadiumId} className="overflow-hidden border-slate-200 hover:shadow-md transition-shadow">
              <div className="relative h-48 bg-muted">
                {venue.imageUrls && venue.imageUrls.length > 0 ? (
                  <Image
                    src={venue.imageUrls[0]}
                    alt={venue.stadiumName}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-muted-foreground">
                    <ImageOff className="h-12 w-12" />
                  </div>
                )}
                <div className="absolute top-3 right-3 flex flex-col gap-1.5 items-end">
                  {getStatusBadge(venue.stadiumStatus)}
                  {getApprovedStatusBadge(venue.approvedStatus)}
                </div>
              </div>

              <CardContent className="p-6">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-bold text-lg mb-1 truncate">{venue.stadiumName}</h3>
                    <Badge variant="outline" className="font-normal">{venue.sportName}</Badge>
                  </div>

                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="h-8 w-8">
                        <MoreVertical className="h-5 w-5" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-48">
                      <DropdownMenuItem onClick={() => router.push(`/owner/venues/${venue.stadiumId}/edit`)}>
                        <Edit className="mr-2 h-4 w-4" />
                        Chỉnh sửa
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => router.push(`/owner/venues/${venue.stadiumId}/slots`)}>
                        <CalendarDays className="mr-2 h-4 w-4" />
                        Cấu hình lịch
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
                        <DropdownMenuItem 
                          className="text-destructive focus:bg-destructive focus:text-destructive-foreground" 
                          onClick={() => setDeleteVenue({ id: venue.stadiumId, name: venue.stadiumName })}
                        >
                          <Trash className="mr-2 h-4 w-4" />
                          Xóa sân
                        </DropdownMenuItem>
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>

                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <p className="truncate" title={venue.address}>{venue.address}</p>
                  <p className="text-primary font-semibold text-base">
                    {venue.pricePerHour.toLocaleString('vi-VN')}đ / giờ
                  </p>
                </div>

                <div className="flex gap-2 mt-4">
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 font-medium"
                    onClick={() => router.push(`/owner/venues/${venue.stadiumId}/slots`)}
                  >
                    <CalendarDays className="mr-1.5 h-4 w-4" />
                    Lịch sân
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 font-medium text-primary hover:text-primary hover:bg-primary/5 border-primary/20"
                    onClick={() => {
                      setSelectedVenueForAccessory({ id: venue.stadiumId, name: venue.stadiumName });
                      setIsAccessoryOpen(true);
                    }}
                  >
                    <Package className="mr-1.5 h-4 w-4" />
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

      <ConfirmDialog
        isOpen={isConfirmOpen}
        onClose={closeConfirm}
        onConfirm={executeConfirm}
        isLoading={isActionLoading}
        title={confirmOptions?.title || ""}
        description={confirmOptions?.description || ""}
        confirmText={confirmOptions?.confirmText}
        variant={confirmOptions?.variant}
      />
    </div>
  );
}

export default VenueManagementPage;
