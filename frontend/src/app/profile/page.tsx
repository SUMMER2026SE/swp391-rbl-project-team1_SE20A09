'use client'

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import {
  Edit,
  Camera,
  Trophy,
  Star,
  Calendar,
  Settings,
} from "lucide-react";

export function UserProfilePage() {
  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        {/* Cover Photo & Avatar */}
        <Card className="mb-6 overflow-hidden">
          <div className="relative">
            <div className="h-48 bg-gradient-to-r from-primary/20 to-primary/10" />
            <Button
              variant="ghost"
              size="sm"
              className="absolute top-4 right-4 bg-white/80"
            >
              <Camera className="h-4 w-4 mr-2" />
              Đổi ảnh bìa
            </Button>

            <div className="absolute left-8 -bottom-16">
              <div className="relative">
                <Avatar className="h-32 w-32 border-4 border-white">
                  <AvatarImage src="https://api.dicebear.com/7.x/avataaars/svg?seed=user" />
                  <AvatarFallback>NVA</AvatarFallback>
                </Avatar>
                <Button
                  size="sm"
                  className="absolute bottom-0 right-0 rounded-full h-10 w-10 p-0"
                >
                  <Camera className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>

          <CardContent className="pt-20 pb-6">
            <div className="flex items-start justify-between">
              <div>
                <h1 className="text-2xl mb-1">Nguyễn Văn A</h1>
                <p className="text-muted-foreground mb-2">
                  Thành viên từ tháng 01/2024
                </p>
                <div className="flex items-center gap-2">
                  <Badge className="bg-amber-100 text-amber-700">
                    <Trophy className="h-3 w-3 mr-1" />
                    Hạng Vàng
                  </Badge>
                  <span className="text-sm text-muted-foreground">
                    1,250 điểm
                  </span>
                </div>
              </div>
              <Button>
                <Edit className="h-4 w-4 mr-2" />
                Chỉnh sửa
              </Button>
            </div>
          </CardContent>
        </Card>

        <Tabs defaultValue="info">
          <TabsList className="mb-6">
            <TabsTrigger value="info">Thông tin</TabsTrigger>
            <TabsTrigger value="bookings">Lịch sử đặt sân</TabsTrigger>
            <TabsTrigger value="reviews">Đánh giá</TabsTrigger>
            <TabsTrigger value="settings">Cài đặt</TabsTrigger>
          </TabsList>

          {/* Info Tab */}
          <TabsContent value="info" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Personal Info */}
              <div className="lg:col-span-2">
                <Card>
                  <CardHeader>
                    <h3>Thông tin cá nhân</h3>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="fullname">Họ và tên</Label>
                        <Input id="fullname" defaultValue="Nguyễn Văn A" />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="phone">Số điện thoại</Label>
                        <Input id="phone" defaultValue="0901234567" />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="email">Email</Label>
                      <Input
                        id="email"
                        type="email"
                        defaultValue="nguyenvana@email.com"
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="address">Địa chỉ</Label>
                      <Input
                        id="address"
                        defaultValue="123 Đường ABC, Quận 1, TP.HCM"
                      />
                    </div>

                    <Button>Lưu thay đổi</Button>
                  </CardContent>
                </Card>
              </div>

              {/* Loyalty Points */}
              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <h3>Điểm thành viên</h3>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="text-center">
                      <div className="text-4xl text-primary mb-2">1,250</div>
                      <p className="text-sm text-muted-foreground">
                        Tổng điểm hiện có
                      </p>
                    </div>

                    <Separator />

                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm">Tiến độ lên hạng</span>
                        <span className="text-sm text-muted-foreground">
                          750/2000
                        </span>
                      </div>
                      <Progress value={37.5} className="h-2" />
                      <p className="text-xs text-muted-foreground mt-2">
                        Còn 750 điểm để lên <strong>Hạng Bạch Kim</strong>
                      </p>
                    </div>

                    <Separator />

                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-muted-foreground">Hạng hiện tại</span>
                        <Badge className="bg-amber-100 text-amber-700">
                          Vàng
                        </Badge>
                      </div>
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-muted-foreground">
                          Ưu đãi giảm giá
                        </span>
                        <span>10%</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <h3>Thống kê</h3>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 text-muted-foreground">
                        <Calendar className="h-4 w-4" />
                        <span className="text-sm">Tổng đặt sân</span>
                      </div>
                      <span>42</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 text-muted-foreground">
                        <Star className="h-4 w-4" />
                        <span className="text-sm">Đánh giá</span>
                      </div>
                      <span>15</span>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </TabsContent>

          {/* Bookings Tab */}
          <TabsContent value="bookings">
            <Card>
              <CardContent className="p-6 text-center text-muted-foreground">
                <Calendar className="h-12 w-12 mx-auto mb-3 opacity-50" />
                <p>Xem lịch sử đặt sân tại trang Lịch sử đặt sân</p>
                <Button variant="link" className="mt-2">
                  Đi tới Lịch sử đặt sân
                </Button>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Reviews Tab */}
          <TabsContent value="reviews">
            <Card>
              <CardContent className="p-6 text-center text-muted-foreground">
                <Star className="h-12 w-12 mx-auto mb-3 opacity-50" />
                <p>Bạn đã viết 15 đánh giá</p>
                <Button variant="link" className="mt-2">
                  Xem tất cả đánh giá
                </Button>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Settings Tab */}
          <TabsContent value="settings">
            <Card>
              <CardHeader>
                <h3>Cài đặt tài khoản</h3>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <h4 className="mb-4">Bảo mật</h4>
                  <div className="space-y-3">
                    <Button variant="outline" className="w-full justify-start">
                      <Settings className="h-4 w-4 mr-2" />
                      Đổi mật khẩu
                    </Button>
                    <Button variant="outline" className="w-full justify-start">
                      <Settings className="h-4 w-4 mr-2" />
                      Xác thực hai yếu tố
                    </Button>
                  </div>
                </div>

                <Separator />

                <div>
                  <h4 className="mb-4">Thông báo</h4>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <Label>Email thông báo đặt sân</Label>
                      <input type="checkbox" defaultChecked />
                    </div>
                    <div className="flex items-center justify-between">
                      <Label>Thông báo khuyến mãi</Label>
                      <input type="checkbox" defaultChecked />
                    </div>
                    <div className="flex items-center justify-between">
                      <Label>Nhắc nhở trước giờ đá</Label>
                      <input type="checkbox" defaultChecked />
                    </div>
                  </div>
                </div>

                <Separator />

                <Button variant="destructive" className="w-full">
                  Xóa tài khoản
                </Button>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>

      <Footer />
    </div>
  );
}


export default UserProfilePage;
