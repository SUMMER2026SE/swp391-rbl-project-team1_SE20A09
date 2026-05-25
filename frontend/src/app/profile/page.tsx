'use client'

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import {
  Edit,
  Trophy,
  Star,
  Calendar,
  Settings,
  Loader2,
} from "lucide-react";
import { get } from "@/lib/api";
import type { UserProfile } from "@/types/user";

const RANK_LABELS: Record<string, string> = {
  Bronze: "Đồng",
  Silver: "Bạc",
  Gold: "Vàng",
  Platinum: "Bạch Kim",
};

function formatMemberSince() {
  return "Thành viên SportHub";
}

function UserProfilePage() {
  const router = useRouter();
  const { data: session, status } = useSession();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login?callbackUrl=/profile");
      return;
    }

    if (status !== "authenticated") return;

    const loadProfile = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await get<UserProfile>("/auth/me");
        setProfile(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Không thể tải hồ sơ.");
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [status, router]);

  const displayProfile = profile ?? session?.user;
  const fullName = displayProfile
    ? `${displayProfile.firstName} ${displayProfile.lastName}`.trim()
    : "";
  const initials = displayProfile
    ? `${displayProfile.firstName?.charAt(0) ?? ""}${displayProfile.lastName?.charAt(0) ?? ""}`.toUpperCase() || "U"
    : "U";
  const rankLabel =
    profile?.userRank && RANK_LABELS[profile.userRank]
      ? RANK_LABELS[profile.userRank]
      : profile?.userRank ?? "—";
  const userPoint = profile?.userPoint ?? 0;
  const nextRankTarget = 2000;
  const progressPercent = Math.min(100, (userPoint / nextRankTarget) * 100);

  if (status === "loading" || isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        {error && (
          <div className="mb-4 p-3 bg-red-100 dark:bg-red-950/50 text-red-600 text-sm rounded-lg border border-red-200">
            {error}
          </div>
        )}

        <Card className="mb-6 overflow-hidden">
          <div className="relative">
            <div className="h-48 bg-gradient-to-r from-primary/20 to-primary/10" />
            <div className="absolute left-8 -bottom-16">
              <Avatar className="h-32 w-32 border-4 border-white">
                <AvatarImage src={displayProfile?.avatarUrl ?? undefined} />
                <AvatarFallback>{initials}</AvatarFallback>
              </Avatar>
            </div>
          </div>

          <CardContent className="pt-20 pb-6">
            <div className="flex items-start justify-between flex-wrap gap-4">
              <div>
                <h1 className="text-2xl mb-1">{fullName || "Hồ sơ của tôi"}</h1>
                <p className="text-muted-foreground mb-2">{formatMemberSince()}</p>
                <div className="flex items-center gap-2">
                  <Badge className="bg-amber-100 text-amber-700">
                    <Trophy className="h-3 w-3 mr-1" />
                    Hạng {rankLabel}
                  </Badge>
                  <span className="text-sm text-muted-foreground">
                    {userPoint.toLocaleString("vi-VN")} điểm
                  </span>
                </div>
              </div>
              <Button asChild>
                <Link href="/profile/edit">
                  <Edit className="h-4 w-4 mr-2" />
                  Chỉnh sửa
                </Link>
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

          <TabsContent value="info" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-2">
                <Card>
                  <CardHeader>
                    <h3>Thông tin cá nhân</h3>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <Label className="text-muted-foreground">Họ và tên</Label>
                        <p className="font-medium mt-1">{fullName}</p>
                      </div>
                      <div>
                        <Label className="text-muted-foreground">Số điện thoại</Label>
                        <p className="font-medium mt-1">
                          {displayProfile?.phoneNumber ?? "—"}
                        </p>
                      </div>
                    </div>
                    <div>
                      <Label className="text-muted-foreground">Email</Label>
                      <p className="font-medium mt-1">{displayProfile?.email ?? "—"}</p>
                    </div>
                    <Button asChild variant="outline">
                      <Link href="/profile/edit">Chỉnh sửa thông tin</Link>
                    </Button>
                  </CardContent>
                </Card>
              </div>

              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <h3>Điểm thành viên</h3>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="text-center">
                      <div className="text-4xl text-primary mb-2">
                        {userPoint.toLocaleString("vi-VN")}
                      </div>
                      <p className="text-sm text-muted-foreground">Tổng điểm hiện có</p>
                    </div>
                    <Separator />
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm">Tiến độ lên hạng</span>
                        <span className="text-sm text-muted-foreground">
                          {userPoint}/{nextRankTarget}
                        </span>
                      </div>
                      <Progress value={progressPercent} className="h-2" />
                    </div>
                    <Separator />
                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-muted-foreground">Hạng hiện tại</span>
                        <Badge className="bg-amber-100 text-amber-700">{rankLabel}</Badge>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </TabsContent>

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

          <TabsContent value="reviews">
            <Card>
              <CardContent className="p-6 text-center text-muted-foreground">
                <Star className="h-12 w-12 mx-auto mb-3 opacity-50" />
                <p>Đánh giá sẽ được cập nhật sau</p>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="settings">
            <Card>
              <CardHeader>
                <h3>Cài đặt tài khoản</h3>
              </CardHeader>
              <CardContent className="space-y-4">
                <Button variant="outline" className="w-full justify-start" asChild>
                  <Link href="/profile/edit">
                    <Settings className="h-4 w-4 mr-2" />
                    Chỉnh sửa hồ sơ
                  </Link>
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
