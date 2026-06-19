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

export interface AdminOwnerResponse {
  ownerId: number;
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  businessName: string;
  taxCode: string;
  businessAddress: string;
  approvedStatus: "PENDING" | "APPROVED" | "REJECTED";
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

const fetchOwners = async (page: number, size: number, search: string, accountStatus: string, approvedStatus: string) => {
  const params: Record<string, string | number> = { page: page + 1, size }; // Spring Boot is 1-indexed, but UI usually 0-indexed for state
  if (search) params.search = search;
  if (accountStatus && accountStatus !== "ALL") params.accountStatus = accountStatus;
  if (approvedStatus && approvedStatus !== "ALL") params.approvedStatus = approvedStatus;

  const { data } = await api.get<ApiResponse<PageResponse<AdminOwnerResponse>>>("/admin/owners", { params });
  return data.result;
};

export default function AdminOwnersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [accountStatusFilter, setAccountStatusFilter] = useState("ALL");
  const [approvedStatusFilter, setApprovedStatusFilter] = useState("ALL");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-owners", page, search, accountStatusFilter, approvedStatusFilter],
    queryFn: () => fetchOwners(page, 10, search, accountStatusFilter, approvedStatusFilter),
  });

  return (
    <div className="p-6 space-y-6 bg-gray-50/50 min-h-screen">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900">Quản lý Chủ Sân</h1>
      </div>

      <div className="flex flex-wrap items-center gap-4 bg-white p-4 rounded-lg shadow-sm border border-gray-100">
        <div className="relative flex-1 min-w-[250px] max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-500" />
          <Input
            placeholder="Tìm theo tên, email, SĐT, doanh nghiệp..."
            className="pl-9 bg-gray-50"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>
        <Select
          value={accountStatusFilter}
          onValueChange={(val) => {
            setAccountStatusFilter(val);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-[200px] bg-gray-50">
            <SelectValue placeholder="Trạng thái tài khoản" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tất cả tài khoản</SelectItem>
            <SelectItem value="ACTIVE">Hoạt động (Active)</SelectItem>
            <SelectItem value="BLOCKED">Đã khóa (Locked)</SelectItem>
          </SelectContent>
        </Select>

        <Select
          value={approvedStatusFilter}
          onValueChange={(val) => {
            setApprovedStatusFilter(val);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-[200px] bg-gray-50">
            <SelectValue placeholder="Trạng thái duyệt" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tất cả hồ sơ</SelectItem>
            <SelectItem value="PENDING">Chờ duyệt (Pending)</SelectItem>
            <SelectItem value="APPROVED">Đã duyệt (Approved)</SelectItem>
            <SelectItem value="REJECTED">Bị từ chối (Rejected)</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <Table>
          <TableHeader className="bg-gray-50/80">
            <TableRow>
              <TableHead className="font-semibold text-gray-900">Họ và Tên</TableHead>
              <TableHead className="font-semibold text-gray-900">Liên hệ</TableHead>
              <TableHead className="font-semibold text-gray-900">Doanh nghiệp</TableHead>
              <TableHead className="font-semibold text-gray-900">Hồ sơ</TableHead>
              <TableHead className="font-semibold text-gray-900">Tài khoản</TableHead>
              <TableHead className="font-semibold text-gray-900">Ngày đăng ký</TableHead>
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
                <TableRow key={owner.ownerId} className="hover:bg-gray-50/50 transition-colors">
                  <TableCell className="font-medium text-gray-900">{owner.fullName}</TableCell>
                  <TableCell className="text-gray-600">
                    <div>{owner.email}</div>
                    <div className="text-xs text-gray-500">{owner.phoneNumber || "N/A"}</div>
                  </TableCell>
                  <TableCell className="text-gray-600">
                    <div className="font-medium">{owner.businessName || "N/A"}</div>
                    <div className="text-xs text-gray-500">MST: {owner.taxCode || "N/A"}</div>
                  </TableCell>
                  <TableCell>
                    {owner.approvedStatus === "APPROVED" ? (
                      <Badge variant="default" className="bg-emerald-500 hover:bg-emerald-600 text-white">Approved</Badge>
                    ) : owner.approvedStatus === "REJECTED" ? (
                      <Badge variant="destructive" className="bg-rose-500">Rejected</Badge>
                    ) : (
                      <Badge variant="secondary" className="bg-amber-400 text-amber-900 hover:bg-amber-500">Pending</Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    {owner.accountStatus === "ACTIVE" ? (
                      <Badge variant="default" className="bg-emerald-500 hover:bg-emerald-600 text-white">Active</Badge>
                    ) : owner.accountStatus === "BLOCKED" ? (
                      <Badge variant="destructive" className="bg-rose-500">Locked</Badge>
                    ) : (
                      <Badge variant="secondary">Pending</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-gray-600">
                    {owner.createdAt
                      ? format(new Date(owner.createdAt), "dd/MM/yyyy")
                      : "N/A"}
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
