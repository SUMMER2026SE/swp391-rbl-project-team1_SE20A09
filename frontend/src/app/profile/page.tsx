'use client'

import { useState, useEffect } from "react";
import { useSession, signOut } from "next-auth/react";
import { useRouter } from "next/navigation";
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
import { get } from "@/lib/api";
import {
  Camera,
  Trophy,
  Star,
  Calendar,
  Settings,
  Loader2,
  User as UserIcon,
  Phone,
  Mail,
  Shield,
  Activity
} from "lucide-react";

interface UserProfileResponse {
  userId: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  avatarUrl?: string;
  roleName: string;
  userPoint: number;
  userRank: string;
  accountStatus: string;
  createdAt: string;
}

const rankMap: Record<string, { label: string; color: string; bg: string; next: string; target: number }> = {
  Bronze: { label: "Hạng Đồng", color: "text-orange-700 border-orange-200", bg: "bg-orange-50", next: "Bạc", target: 200 },
  Silver: { label: "Hạng Bạc", color: "text-slate-600 border-slate-200", bg: "bg-slate-50", next: "Vàng", target: 500 },
  Gold: { label: "Hạng Vàng", color: "text-yellow-700 border-yellow-200", bg: "bg-yellow-50", next: "Bạch Kim", target: 1000 },
  Platinum: { label: "Hạng Bạch Kim", color: "text-emerald-700 border-emerald-200", bg: "bg-emerald-50", next: "Kim Cương", target: 2000 },
  Diamond: { label: "Hạng Kim Cương", color: "text-blue-700 border-blue-200", bg: "bg-blue-50", next: "Vô Song", target: 5000 },
};

function UserProfilePage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/login");
      return;
    }

    if (status === "authenticated") {
      fetchUserProfile();
    }
  }, [status, router]);

  const fetchUserProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await get<UserProfileResponse>("/users/me");
      setProfile(data);
    } catch (err: any) {
      console.error("Error fetching user profile:", err);
      if (err.status === 401 || err.message?.includes("401") || err.message?.includes("unauthorized") || err.message?.includes("authenticated")) {
        console.warn("Session expired or unauthorized, signing out...");
        signOut({ callbackUrl: "/login?error=SessionExpired" });
        return;
      }
      setError(err.message ?? "Không thể tải thông tin hồ sơ. Vui lòng thử lại sau.");
    } finally {
      setLoading(false);
    }
  };

  // Get initials for avatar fallback
  const getInitials = (name: string) => {
    if (!name) return "U";
    const parts = name.split(" ");
    if (parts.length >= 2) {
      return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  if (status === "loading" || loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col">
        <Header />
        <div className="flex-1 flex flex-col items-center justify-center space-y-4 p-8">
          <Loader2 className="h-10 w-10 text-primary animate-spin" />
          <p className="text-muted-foreground animate-pulse text-sm">
            Đang tải thông tin tài khoản của bạn...
          </p>
        </div>
        <Footer />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="min-h-screen bg-background flex flex-col">
        <Header />
        <div className="flex-1 container mx-auto px-4 py-16 flex flex-col items-center justify-center text-center">
          <Card className="max-w-md w-full p-8 border border-red-100 shadow-sm">
            <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <Shield className="h-8 w-8 text-red-500" />
            </div>
            <h2 className="text-xl font-bold mb-2">Đã có lỗi xảy ra</h2>
            <p className="text-muted-foreground text-sm mb-6">
              {error ?? "Đã xảy ra lỗi không xác định khi truy xuất thông tin của bạn."}
            </p>
            <Button onClick={fetchUserProfile} className="w-full">
              Thử tải lại trang
            </Button>
          </Card>
        </div>
        <Footer />
      </div>
    );
  }

  // Calculate rank info
  const currentRankInfo = rankMap[profile.userRank] || {
    label: profile.userRank,
    color: "text-primary",
    bg: "bg-primary/10",
    next: "Cao Hơn",
    target: profile.userPoint * 1.5
  };

  const points = profile.userPoint;
  const targetPoints = currentRankInfo.target;
  const progressPercent = Math.min((points / targetPoints) * 100, 100);

  const formattedJoinDate = profile.createdAt
    ? new Date(profile.createdAt).toLocaleDateString("vi-VN", {
        month: "2-digit",
        year: "numeric"
      })
    : "01/2024";

  return (
    <div className="min-h-screen bg-slate-50/50 flex flex-col">
      <Header />

      <main className="flex-1 container mx-auto px-4 py-8">
        {/* Cover Photo & Avatar Card */}
        <Card className="mb-8 overflow-hidden border-none shadow-md bg-white">
          <div className="relative">
            {/* Design elements */}
            <div className="h-48 bg-gradient-to-r from-emerald-600 via-teal-600 to-cyan-600 relative overflow-hidden">
              <div className="absolute inset-0 opacity-10 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-white via-transparent to-transparent"></div>
              <div className="absolute -right-16 -top-16 w-64 h-64 bg-white/5 rounded-full blur-2xl"></div>
              <div className="absolute -left-16 -bottom-16 w-64 h-64 bg-black/10 rounded-full blur-2xl"></div>
            </div>

            <Button
              variant="ghost"
              size="sm"
              className="absolute top-4 right-4 bg-white/20 hover:bg-white/30 text-white backdrop-blur-md border border-white/10"
            >
              <Camera className="h-4 w-4 mr-2" />
              Đổi ảnh bìa
            </Button>

            <div className="absolute left-8 -bottom-16">
              <div className="relative">
                <Avatar className="h-32 w-32 border-4 border-white shadow-lg bg-white">
                  <AvatarImage src={profile.avatarUrl} alt={profile.fullName} />
                  <AvatarFallback className="bg-gradient-to-tr from-emerald-500 to-teal-500 text-white text-3xl font-extrabold">
                    {getInitials(profile.fullName)}
                  </AvatarFallback>
                </Avatar>
                <Button
                  size="sm"
                  className="absolute bottom-0 right-0 rounded-full h-9 w-9 p-0 shadow-md border-2 border-white bg-primary hover:bg-primary/90"
                >
                  <Camera className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>

          <CardContent className="pt-20 pb-8 px-8">
            <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-3">
                  <h1 className="text-3xl font-extrabold tracking-tight text-slate-800">
                    {profile.fullName}
                  </h1>
                  <span className="px-2.5 py-0.5 text-xs font-semibold rounded bg-primary/10 text-primary border border-primary/20 uppercase tracking-wider">
                    {profile.roleName}
                  </span>
                </div>
                <p className="text-slate-500 text-sm flex items-center gap-1.5">
                  <Calendar className="h-4 w-4 text-slate-400" />
                  Thành viên từ tháng {formattedJoinDate}
                </p>
                <div className="flex items-center gap-2 pt-2">
                  <Badge className={`${currentRankInfo.bg} ${currentRankInfo.color} border px-3 py-1 font-semibold flex items-center gap-1`}>
                    <Trophy className="h-3.5 w-3.5" />
                    {currentRankInfo.label}
                  </Badge>
                  <span className="text-sm font-bold text-teal-600 bg-teal-50 border border-teal-100 px-2 py-0.5 rounded">
                    {profile.userPoint.toLocaleString()} điểm
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Button className="shadow-sm">
                  <UserIcon className="h-4 w-4 mr-2" />
                  Hồ sơ của tôi
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Tab Interface */}
        <Tabs defaultValue="info" className="space-y-6">
          <TabsList className="bg-white p-1 rounded-xl shadow-sm border border-slate-100 w-full sm:w-auto overflow-x-auto flex whitespace-nowrap">
            <TabsTrigger value="info" className="px-6 py-2.5 rounded-lg data-[state=active]:bg-primary data-[state=active]:text-primary-foreground transition-all">
              Thông tin cá nhân
            </TabsTrigger>
            <TabsTrigger value="bookings" className="px-6 py-2.5 rounded-lg data-[state=active]:bg-primary data-[state=active]:text-primary-foreground transition-all">
              Lịch sử đặt sân
            </TabsTrigger>
            <TabsTrigger value="reviews" className="px-6 py-2.5 rounded-lg data-[state=active]:bg-primary data-[state=active]:text-primary-foreground transition-all">
              Đánh giá của tôi
            </TabsTrigger>
            <TabsTrigger value="settings" className="px-6 py-2.5 rounded-lg data-[state=active]:bg-primary data-[state=active]:text-primary-foreground transition-all">
              Bảo mật & Cài đặt
            </TabsTrigger>
          </TabsList>

          {/* Info Tab */}
          <TabsContent value="info" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Detailed Personal Info Form */}
              <div className="lg:col-span-2 space-y-6">
                <Card className="border-none shadow-sm bg-white">
                  <CardHeader className="pb-4">
                    <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                      <UserIcon className="h-5 w-5 text-primary" />
                      Chi tiết tài khoản
                    </h3>
                    <p className="text-slate-500 text-sm">Xem và kiểm tra thông tin tài khoản đã xác thực của bạn.</p>
                  </CardHeader>
                  <CardContent className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="space-y-2">
                        <Label htmlFor="lastName" className="text-slate-600 font-medium">Họ</Label>
                        <Input
                          id="lastName"
                          value={profile.lastName}
                          disabled
                          className="bg-slate-50 border-slate-200 text-slate-700 disabled:opacity-100 disabled:cursor-not-allowed font-medium"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="firstName" className="text-slate-600 font-medium">Tên</Label>
                        <Input
                          id="firstName"
                          value={profile.firstName}
                          disabled
                          className="bg-slate-50 border-slate-200 text-slate-700 disabled:opacity-100 disabled:cursor-not-allowed font-medium"
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="email" className="text-slate-600 font-medium flex items-center gap-1.5">
                        <Mail className="h-4 w-4 text-slate-400" />
                        Địa chỉ Email
                      </Label>
                      <Input
                        id="email"
                        type="email"
                        value={profile.email}
                        disabled
                        className="bg-slate-50 border-slate-200 text-slate-700 disabled:opacity-100 disabled:cursor-not-allowed font-medium"
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="phone" className="text-slate-600 font-medium flex items-center gap-1.5">
                        <Phone className="h-4 w-4 text-slate-400" />
                        Số điện thoại
                      </Label>
                      <Input
                        id="phone"
                        value={profile.phoneNumber || "Chưa cập nhật"}
                        disabled
                        className="bg-slate-50 border-slate-200 text-slate-700 disabled:opacity-100 disabled:cursor-not-allowed font-medium"
                      />
                    </div>

                    <div className="pt-2 flex items-center justify-between border-t border-slate-100">
                      <div className="flex items-center gap-2">
                        <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200 font-medium">
                          <Activity className="h-3 w-3 mr-1" />
                          Trạng thái: {profile.accountStatus === "Active" ? "Đang hoạt động" : "Tạm khóa"}
                        </Badge>
                      </div>
                      <p className="text-xs text-slate-400">
                        *Để thay đổi các thông tin này, vui lòng nhấn nút chỉnh sửa ở phần cài đặt.
                      </p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Loyalty & Rank Progress Card */}
              <div className="space-y-6">
                <Card className="border-none shadow-sm bg-white overflow-hidden relative">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-8 -mt-8"></div>
                  <CardHeader className="pb-2">
                    <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                      <Trophy className="h-5 w-5 text-amber-500" />
                      Hạng & Điểm Tích Lũy
                    </h3>
                  </CardHeader>
                  <CardContent className="space-y-6">
                    <div className="text-center py-4 bg-slate-50 rounded-2xl border border-slate-100">
                      <div className="text-5xl font-extrabold text-primary mb-1">
                        {profile.userPoint.toLocaleString()}
                      </div>
                      <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Điểm tích lũy hiện có
                      </p>
                    </div>

                    <div className="space-y-3">
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-slate-600 font-medium">Hành trình lên {currentRankInfo.next}</span>
                        <span className="font-semibold text-slate-700">
                          {points.toLocaleString()} / {targetPoints.toLocaleString()}
                        </span>
                      </div>
                      
                      <div className="relative">
                        <Progress value={progressPercent} className="h-2.5 bg-slate-100" />
                      </div>
                      
                      {points < targetPoints ? (
                        <p className="text-xs text-slate-500 leading-relaxed">
                          Bạn chỉ cần tích lũy thêm <strong className="text-primary">{(targetPoints - points).toLocaleString()} điểm</strong> để thăng hạng <strong className="text-primary">{currentRankInfo.next}</strong>.
                        </p>
                      ) : (
                        <p className="text-xs text-slate-500 leading-relaxed">
                          Chúc mừng! Bạn đã đạt mức điểm tối đa của bảng xếp hạng hiện tại.
                        </p>
                      )}
                    </div>

                    <Separator className="bg-slate-100" />

                    <div className="space-y-2.5">
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-slate-500">Cấp bậc hiện tại</span>
                        <Badge className={`${currentRankInfo.bg} ${currentRankInfo.color} font-bold`}>
                          {profile.userRank}
                        </Badge>
                      </div>
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-slate-500">Chiết khấu đặc quyền</span>
                        <span className="font-bold text-emerald-600">
                          {profile.userRank === "Bronze" ? "0%" : profile.userRank === "Silver" ? "3%" : profile.userRank === "Gold" ? "5%" : "10%"}
                        </span>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {/* Quick stats */}
                <Card className="border-none shadow-sm bg-white">
                  <CardHeader className="pb-2">
                    <h3 className="text-lg font-bold text-slate-800">Hoạt động</h3>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex items-center justify-between py-2 border-b border-slate-50">
                      <div className="flex items-center gap-2 text-slate-500">
                        <Calendar className="h-4.5 w-4.5 text-primary/75" />
                        <span className="text-sm">Tổng lượt đặt sân</span>
                      </div>
                      <span className="font-bold text-slate-800">12 lượt</span>
                    </div>
                    <div className="flex items-center justify-between py-2 border-b border-slate-50">
                      <div className="flex items-center gap-2 text-slate-500">
                        <Star className="h-4.5 w-4.5 text-amber-400" />
                        <span className="text-sm">Đánh giá đã gửi</span>
                      </div>
                      <span className="font-bold text-slate-800">5 lượt</span>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </TabsContent>

          {/* Bookings Tab */}
          <TabsContent value="bookings">
            <Card className="border-none shadow-sm bg-white p-8 text-center">
              <Calendar className="h-16 w-16 text-slate-300 mx-auto mb-4" />
              <h3 className="text-lg font-bold text-slate-800 mb-2">Xem lịch sử đặt sân</h3>
              <p className="text-slate-500 text-sm max-w-sm mx-auto mb-6">
                Tất cả thông tin về lịch sử đặt sân bóng, hóa đơn giao dịch được quản lý tập trung ở trang Đơn hàng của tôi.
              </p>
              <Button variant="outline" onClick={() => router.push("/bookings")} className="border-slate-200">
                Đi tới Lịch sử đặt sân
              </Button>
            </Card>
          </TabsContent>

          {/* Reviews Tab */}
          <TabsContent value="reviews">
            <Card className="border-none shadow-sm bg-white p-8 text-center">
              <Star className="h-16 w-16 text-amber-300 mx-auto mb-4" />
              <h3 className="text-lg font-bold text-slate-800 mb-2">Đánh giá chất lượng sân</h3>
              <p className="text-slate-500 text-sm max-w-sm mx-auto mb-6">
                Bạn đã thực hiện đánh giá cho 5 lượt đặt sân. Các nhận xét của bạn giúp ích rất nhiều cho cộng đồng người chơi!
              </p>
              <Button variant="outline" className="border-slate-200">
                Xem toàn bộ đánh giá
              </Button>
            </Card>
          </TabsContent>

          {/* Settings Tab */}
          <TabsContent value="settings">
            <Card className="border-none shadow-sm bg-white">
              <CardHeader>
                <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                  <Settings className="h-5 w-5 text-primary" />
                  Cài đặt bảo mật nâng cao
                </h3>
              </CardHeader>
              <CardContent className="space-y-6">
                <div>
                  <h4 className="font-bold text-slate-800 mb-4 text-sm uppercase tracking-wide">Tài khoản & Xác thực</h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Button variant="outline" onClick={() => router.push("/reset-password")} className="justify-start border-slate-200 py-6 h-auto">
                      <Settings className="h-5 w-5 mr-3 text-slate-500" />
                      <div className="text-left">
                        <div className="font-semibold text-slate-800 text-sm">Đổi mật khẩu</div>
                        <div className="text-slate-400 text-xs mt-0.5">Nên thay đổi mật khẩu định kỳ để an toàn</div>
                      </div>
                    </Button>
                    <Button variant="outline" disabled className="justify-start border-slate-200 py-6 h-auto opacity-75">
                      <Shield className="h-5 w-5 mr-3 text-slate-400" />
                      <div className="text-left">
                        <div className="font-semibold text-slate-500 text-sm">Bảo mật 2 lớp (2FA)</div>
                        <div className="text-slate-400 text-xs mt-0.5">Đang phát triển thiết lập an toàn SMS</div>
                      </div>
                    </Button>
                  </div>
                </div>

                <Separator className="bg-slate-100" />

                <div>
                  <h4 className="font-bold text-slate-800 mb-4 text-sm uppercase tracking-wide">Cấu hình Nhận Thông báo</h4>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
                      <div>
                        <Label className="font-bold text-slate-800 text-sm block">Email thông báo đặt sân</Label>
                        <span className="text-xs text-slate-500">Nhận thông tin xác nhận và hóa đơn thanh toán đặt sân</span>
                      </div>
                      <input type="checkbox" defaultChecked className="w-4 h-4 rounded text-primary focus:ring-primary accent-primary" />
                    </div>
                    <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
                      <div>
                        <Label className="font-bold text-slate-800 text-sm block">Thông báo khuyến mãi & Sự kiện</Label>
                        <span className="text-xs text-slate-500">Cập nhật tin ưu đãi Voucher đặt sân mới nhất</span>
                      </div>
                      <input type="checkbox" defaultChecked className="w-4 h-4 rounded text-primary focus:ring-primary accent-primary" />
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>

      <Footer />
    </div>
  );
}

export default UserProfilePage;
