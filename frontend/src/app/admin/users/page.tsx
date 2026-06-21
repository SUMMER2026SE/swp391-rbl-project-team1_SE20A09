'use client'

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Search, Lock, Unlock, Eye, AlertCircle } from "lucide-react";

interface UserMock {
  id: number;
  name: string;
  email: string;
  phone: string;
  avatar: string;
  joinDate: string;
  status: string;
  type: string;
  bookings?: number;
  venues?: number;
  revenue?: number;
}

function UserManagementPage() {
  const [selectedUser, setSelectedUser] = useState<UserMock | null>(null);

  const users = [
    {
      id: 1,
      name: "Nguyễn Văn A",
      email: "nguyenvana@email.com",
      phone: "0901234567",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
      joinDate: "15/01/2024",
      bookings: 42,
      status: "active",
      type: "customer",
    },
    {
      id: 2,
      name: "Trần Thị B",
      email: "tranthib@email.com",
      phone: "0909876543",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=2",
      joinDate: "20/01/2024",
      bookings: 28,
      status: "active",
      type: "customer",
    },
    {
      id: 3,
      name: "Lê Văn C",
      email: "levanc@email.com",
      phone: "0903456789",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=3",
      joinDate: "10/02/2024",
      bookings: 15,
      status: "locked",
      type: "customer",
    },
  ];

  const owners = [
    {
      id: 1,
      name: "Phạm Văn D",
      email: "owner1@email.com",
      phone: "0904567890",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=4",
      joinDate: "05/01/2024",
      venues: 3,
      revenue: 45000000,
      status: "active",
      type: "owner",
    },
    {
      id: 2,
      name: "Hoàng Thị E",
      email: "owner2@email.com",
      phone: "0905678901",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=5",
      joinDate: "12/01/2024",
      venues: 2,
      revenue: 28000000,
      status: "active",
      type: "owner",
    },
  ];

  const getStatusBadge = (status: string) => {
    if (status === "active") {
      return <Badge className="bg-green-100 text-green-700">Hoạt động</Badge>;
    }
    return <Badge className="bg-red-100 text-red-700">Đã khóa</Badge>;
  };

  const UserRow = ({ user }: { user: UserMock }) => (
    <tr className="border-b hover:bg-muted">
      <td className="p-3">
        <div className="flex items-center gap-3">
          <Avatar>
            <AvatarImage src={user.avatar} />
            <AvatarFallback>{user.name[0]}</AvatarFallback>
          </Avatar>
          <div>
            <div className="font-medium">{user.name}</div>
            <div className="text-sm text-muted-foreground">{user.email}</div>
          </div>
        </div>
      </td>
      <td className="p-3">{user.phone}</td>
      <td className="p-3">{user.joinDate}</td>
      <td className="p-3 text-center">
        {user.type === "customer" ? user.bookings : user.venues}
      </td>
      {user.type === "owner" && (
        <td className="p-3 text-right">{(user.revenue ?? 0).toLocaleString('vi-VN')}đ</td>
      )}
      <td className="p-3">{getStatusBadge(user.status)}</td>
      <td className="p-3">
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => setSelectedUser(user)}
          >
            <Eye className="h-4 w-4" />
          </Button>
          {user.status === "active" ? (
            <Button size="sm" variant="destructive">
              <Lock className="h-4 w-4" />
            </Button>
          ) : (
            <Button size="sm" variant="default">
              <Unlock className="h-4 w-4" />
            </Button>
          )}
        </div>
      </td>
    </tr>
  );

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl mb-8">Quản lý người dùng</h1>

        <Tabs defaultValue="customers">
          <div className="flex items-center justify-between mb-6">
            <TabsList>
              <TabsTrigger value="customers">
                Khách hàng ({users.length})
              </TabsTrigger>
              <TabsTrigger value="owners">
                Chủ sân ({owners.length})
              </TabsTrigger>
            </TabsList>

            <div className="flex gap-4">
              <div className="relative w-64">
                <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input placeholder="Tìm kiếm..." className="pl-9" />
              </div>
              <Select defaultValue="all">
                <SelectTrigger className="w-48">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả trạng thái</SelectItem>
                  <SelectItem value="active">Hoạt động</SelectItem>
                  <SelectItem value="locked">Đã khóa</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <Card>
            <CardContent className="p-0">
              <TabsContent value="customers" className="m-0">
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-muted">
                      <tr>
                        <th className="p-3 text-left">Người dùng</th>
                        <th className="p-3 text-left">Số điện thoại</th>
                        <th className="p-3 text-left">Ngày tham gia</th>
                        <th className="p-3 text-center">Số lượt đặt</th>
                        <th className="p-3 text-left">Trạng thái</th>
                        <th className="p-3 text-left">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map((user) => (
                        <UserRow key={user.id} user={user} />
                      ))}
                    </tbody>
                  </table>
                </div>
              </TabsContent>

              <TabsContent value="owners" className="m-0">
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-muted">
                      <tr>
                        <th className="p-3 text-left">Chủ sân</th>
                        <th className="p-3 text-left">Số điện thoại</th>
                        <th className="p-3 text-left">Ngày tham gia</th>
                        <th className="p-3 text-center">Số sân</th>
                        <th className="p-3 text-right">Doanh thu</th>
                        <th className="p-3 text-left">Trạng thái</th>
                        <th className="p-3 text-left">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      {owners.map((owner) => (
                        <UserRow key={owner.id} user={owner} />
                      ))}
                    </tbody>
                  </table>
                </div>
              </TabsContent>
            </CardContent>
          </Card>
        </Tabs>

        {/* User Detail Sheet */}
        <Sheet open={!!selectedUser} onOpenChange={() => setSelectedUser(null)}>
          <SheetContent>
            <SheetHeader>
              <SheetTitle>Chi tiết người dùng</SheetTitle>
            </SheetHeader>

            {selectedUser && (
              <div className="mt-6 space-y-6">
                <div className="flex items-center gap-4">
                  <Avatar className="h-20 w-20">
                    <AvatarImage src={selectedUser.avatar} />
                    <AvatarFallback>{selectedUser.name[0]}</AvatarFallback>
                  </Avatar>
                  <div>
                    <h3>{selectedUser.name}</h3>
                    {getStatusBadge(selectedUser.status)}
                  </div>
                </div>

                <div className="space-y-3">
                  <div>
                    <div className="text-sm text-muted-foreground">Email</div>
                    <div>{selectedUser.email}</div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">
                      Số điện thoại
                    </div>
                    <div>{selectedUser.phone}</div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">
                      Ngày tham gia
                    </div>
                    <div>{selectedUser.joinDate}</div>
                  </div>

                  {selectedUser.type === "customer" && (
                    <div>
                      <div className="text-sm text-muted-foreground">
                        Tổng số lượt đặt
                      </div>
                      <div>{selectedUser.bookings}</div>
                    </div>
                  )}

                  {selectedUser.type === "owner" && (
                    <>
                      <div>
                        <div className="text-sm text-muted-foreground">
                          Số lượng sân
                        </div>
                        <div>{selectedUser.venues}</div>
                      </div>
                      <div>
                        <div className="text-sm text-muted-foreground">
                          Doanh thu tháng này
                        </div>
                        <div className="text-xl text-primary">
                          {(selectedUser.revenue ?? 0).toLocaleString('vi-VN')}đ
                        </div>
                      </div>
                    </>
                  )}
                </div>

                <div className="space-y-2">
                  {selectedUser.status === "active" ? (
                    <Button variant="destructive" className="w-full">
                      <Lock className="h-4 w-4 mr-2" />
                      Khóa tài khoản
                    </Button>
                  ) : (
                    <Button variant="default" className="w-full">
                      <Unlock className="h-4 w-4 mr-2" />
                      Mở khóa tài khoản
                    </Button>
                  )}
                  <Button variant="outline" className="w-full">
                    <AlertCircle className="h-4 w-4 mr-2" />
                    Xem lịch sử khiếu nại
                  </Button>
                </div>
              </div>
            )}
          </SheetContent>
        </Sheet>
    </div>
  );
}

export default UserManagementPage;
