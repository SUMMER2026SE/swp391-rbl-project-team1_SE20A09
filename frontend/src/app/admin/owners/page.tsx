"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { format } from "date-fns";
import { Search, Loader2 } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useDebounceValue } from "usehooks-ts";


export interface AdminOwnerResponse {
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  accountStatus: "ACTIVE" | "BLOCKED" | "PENDING";
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

interface CreateOwnerRequest {
  fullName: string;
  email: string;
  phoneNumber?: string;
  phone?: string;
  password?: string;
  confirmPassword?: string;
}

interface ApiError {
  response?: {
    data?: { message?: string } | Record<string, string>;
    status?: number;
  };
  message?: string;
}

const fetchOwners = async (page: number, size: number, search: string, status: string) => {
  const params: Record<string, string | number> = { page, pageSize: size };
  if (search) params.search = search;
  if (status && status !== "ALL") params.accountStatus = status;

  const { data } = await api.get<ApiResponse<PageResponse<AdminOwnerResponse>>>("/admin/owners", { params });
  return data.result;
};



export default function AdminOwnersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newOwner, setNewOwner] = useState({ fullName: "", email: "", phoneNumber: "", password: "", confirmPassword: "" });

  const [debouncedSearch] = useDebounceValue(search, 500);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-owners", page, debouncedSearch, statusFilter],
    queryFn: () => fetchOwners(page, 10, debouncedSearch, statusFilter),
  });

  const addOwnerMutation = useMutation({
    mutationFn: async (ownerData: CreateOwnerRequest) => {
      // Gọi API register của hệ thống
      const { data } = await api.post("/auth/register", ownerData);
      return data;
    },
    onSuccess: () => {
      toast.success("Thêm chủ sân thành công! Đã gửi email xác thực.");
      queryClient.invalidateQueries({ queryKey: ["admin-owners"] });
      setIsAddModalOpen(false);
      setNewOwner({ fullName: "", email: "", phoneNumber: "", password: "", confirmPassword: "" });
    },
    onError: (error: ApiError) => {
      if (error.response?.data && typeof error.response.data === 'object' && !error.response.data.message) {
         const msgs = Object.values(error.response.data).join("; ");
         toast.error(msgs || "Lỗi dữ liệu đầu vào");
      } else {
         toast.error(error?.response?.data?.message || error?.message || "Có lỗi xảy ra khi thêm chủ sân.");
      }
    }
  });

  const handleAddOwner = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newOwner.email || !newOwner.fullName || !newOwner.password) {
      toast.error("Vui lòng nhập đủ họ tên, email và mật khẩu!");
      return;
    }
    if (newOwner.password !== newOwner.confirmPassword) {
      toast.error("Mật khẩu xác nhận không khớp!");
      return;
    }
    addOwnerMutation.mutate({
      fullName: newOwner.fullName,
      email: newOwner.email,
      phone: newOwner.phoneNumber,
      password: newOwner.password,
      confirmPassword: newOwner.confirmPassword
    });
  };

  const lockUnlockMutation = useMutation({
    mutationFn: async ({ id, enabled }: { id: number; enabled: boolean }) => {
      const { data } = await api.patch(`/admin/owners/${id}/lock`, { enabled });
      return data;
    },
    onSuccess: (data) => {
      toast.success(data.message || "Thao tác thành công");
      queryClient.invalidateQueries({ queryKey: ["admin-owners"] });
    },
    onError: (error: ApiError) => {
      toast.error(error?.response?.data?.message || error?.message || "Có lỗi xảy ra.");
    }
  });

  const handleLockUnlock = (owner: AdminOwnerResponse) => {
    const isCurrentlyActive = owner.accountStatus === "ACTIVE";
    const action = isCurrentlyActive ? "khóa" : "mở khóa";
    
    if (confirm(`Bạn có chắc chắn muốn ${action} tài khoản ${owner.email}?`)) {
      lockUnlockMutation.mutate({ id: owner.userId, enabled: !isCurrentlyActive });
    }
  };

  return (
    <div className="p-6 space-y-6 bg-gray-50/50 min-h-screen">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900">Quản lý Chủ sân</h1>
        
        <Dialog open={isAddModalOpen} onOpenChange={setIsAddModalOpen}>
          <DialogTrigger asChild>
            <Button className="bg-primary text-white shadow-sm hover:bg-primary/90">
              + Thêm Chủ sân mới
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-[425px]">
            <form onSubmit={handleAddOwner}>
              <DialogHeader>
                <DialogTitle>Thêm Chủ sân</DialogTitle>
                <DialogDescription>
                  Tạo tài khoản mới cho chủ sân. Vui lòng điền đầy đủ thông tin bên dưới.
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-4 items-center gap-4">
                  <label htmlFor="fullName" className="text-right text-sm font-medium">Họ & Tên</label>
                  <Input id="fullName" className="col-span-3" value={newOwner.fullName} onChange={e => setNewOwner({...newOwner, fullName: e.target.value})} placeholder="Nguyễn Văn A" required />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <label htmlFor="email" className="text-right text-sm font-medium">Email</label>
                  <Input id="email" type="email" className="col-span-3" value={newOwner.email} onChange={e => setNewOwner({...newOwner, email: e.target.value})} placeholder="email@example.com" required />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <label htmlFor="phone" className="text-right text-sm font-medium">SĐT</label>
                  <Input id="phone" className="col-span-3" value={newOwner.phoneNumber} onChange={e => setNewOwner({...newOwner, phoneNumber: e.target.value})} placeholder="0912345678" required />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <label htmlFor="password" className="text-right text-sm font-medium">Mật khẩu</label>
                  <Input id="password" type="password" className="col-span-3" value={newOwner.password} onChange={e => setNewOwner({...newOwner, password: e.target.value})} placeholder="********" required />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <label htmlFor="confirmPassword" className="text-right text-sm font-medium">Xác nhận MK</label>
                  <Input id="confirmPassword" type="password" className="col-span-3" value={newOwner.confirmPassword} onChange={e => setNewOwner({...newOwner, confirmPassword: e.target.value})} placeholder="********" required />
                </div>
              </div>
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setIsAddModalOpen(false)}>Hủy</Button>
                <Button type="submit" disabled={addOwnerMutation.isPending}>
                  {addOwnerMutation.isPending && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  Lưu
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <div className="flex items-center gap-4 bg-white p-4 rounded-lg shadow-sm border border-gray-100">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-500" />
          <Input
            placeholder="Tìm kiếm theo tên, email..."
            className="pl-9 bg-gray-50"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>
        <Select
          value={statusFilter}
          onValueChange={(val) => {
            setStatusFilter(val);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-[200px] bg-gray-50">
            <SelectValue placeholder="Trạng thái" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
            <SelectItem value="ACTIVE">Hoạt động (Active)</SelectItem>
            <SelectItem value="BLOCKED">Đã khóa (Locked)</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <Table>
          <TableHeader className="bg-gray-50/80">
            <TableRow>
              <TableHead className="font-semibold text-gray-900">Tên chủ sân</TableHead>
              <TableHead className="font-semibold text-gray-900">Email</TableHead>
              <TableHead className="font-semibold text-gray-900">Số điện thoại</TableHead>
              <TableHead className="font-semibold text-gray-900">Ngày tạo</TableHead>
              <TableHead className="font-semibold text-gray-900">Trạng thái</TableHead>
              <TableHead className="font-semibold text-gray-900 text-right">Hành động</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={6} className="h-32 text-center">
                  <Loader2 className="h-6 w-6 animate-spin mx-auto text-primary" />
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={6} className="h-32 text-center text-red-500 font-medium">
                  Đã xảy ra lỗi khi tải dữ liệu! Vui lòng thử lại.
                </TableCell>
              </TableRow>
            ) : data?.content?.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="h-32 text-center text-gray-500">
                  Không tìm thấy chủ sân nào phù hợp với bộ lọc.
                </TableCell>
              </TableRow>
            ) : (
              data?.content?.map((owner) => (
                <TableRow key={owner.userId} className="hover:bg-gray-50/50 transition-colors">
                  <TableCell className="font-medium text-gray-900">{owner.fullName}</TableCell>
                  <TableCell className="text-gray-600">{owner.email}</TableCell>
                  <TableCell className="text-gray-600">{owner.phoneNumber || "N/A"}</TableCell>
                  <TableCell className="text-gray-600">
                    {owner.createdAt
                      ? format(new Date(owner.createdAt), "dd/MM/yyyy HH:mm")
                      : "N/A"}
                  </TableCell>
                  <TableCell>
                    {owner.accountStatus === "ACTIVE" ? (
                      <Badge variant="default" className="bg-emerald-500 hover:bg-emerald-600 text-white">
                        Active
                      </Badge>
                    ) : owner.accountStatus === "BLOCKED" ? (
                      <Badge variant="destructive" className="bg-rose-500">Locked</Badge>
                    ) : (
                      <Badge variant="secondary">Pending</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant={owner.accountStatus === "ACTIVE" ? "destructive" : "default"}
                      size="sm"
                      onClick={() => handleLockUnlock(owner)}
                      disabled={lockUnlockMutation.isPending && lockUnlockMutation.variables?.id === owner.userId}
                      className={owner.accountStatus !== "ACTIVE" ? "bg-emerald-500 hover:bg-emerald-600" : ""}
                    >
                      {lockUnlockMutation.isPending && lockUnlockMutation.variables?.id === owner.userId && (
                        <Loader2 className="w-3 h-3 mr-1 animate-spin" />
                      )}
                      {owner.accountStatus === "ACTIVE" ? "Khóa" : "Mở khóa"}
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex justify-end items-center gap-4">
        <span className="text-sm text-gray-500 font-medium">
          Trang {page + 1} / {data?.totalPages || 1}
        </span>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            className="w-20"
          >
            Trước
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={!data || page >= (data.totalPages - 1)}
            onClick={() => setPage((p) => p + 1)}
            className="w-20"
          >
            Sau
          </Button>
        </div>
      </div>
    </div>
  );
}
