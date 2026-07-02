'use client'

import { useState, useEffect } from "react";
import Link from "next/link";
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
import { get, post, patch } from "@/lib/api";
import { BookingHistoryList } from "@/components/bookings/BookingHistoryList";
import { ReviewHistoryList } from "@/components/reviews/ReviewHistoryList";
import { OwnerReviewHistoryList } from "@/components/reviews/OwnerReviewHistoryList";
import { ComplaintList } from "@/components/complaints/ComplaintList";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { upgradeToOwnerSchema, type UpgradeToOwnerFormValues } from "@/lib/validations/auth.schema";
import { DocumentUploader } from "@/components/shared/DocumentUploader";
import { toast } from "sonner";

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
  Activity,
  Lock,
  Eye,
  EyeOff,
  Edit,
  Clock,
  MapPin,
  FileText,
  AlertTriangle,
  Building,
  CheckCircle2,
  ShieldCheck,
  Bell,
  ToggleRight,
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

interface OwnerDetailResponse {
  ownerId: number;
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  businessName: string;
  taxCode: string;
  businessAddress: string;
  businessLicenseUrl?: string;
  identityCardUrl?: string;
  approvedStatus: "PENDING" | "APPROVED" | "REJECTED";
  rejectionReason?: string;
  createdAt: string;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

function UserProfilePage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reviews, setReviews] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState("info");

  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [changePasswordLoading, setChangePasswordLoading] = useState(false);
  const [changePasswordError, setChangePasswordError] = useState<string | null>(null);
  const [changePasswordSuccess, setChangePasswordSuccess] = useState<string | null>(null);

  // Admin notification preferences (local state — persisted on toggle)
  const [notifOwner, setNotifOwner] = useState(true);
  const [notifStadium, setNotifStadium] = useState(true);
  const [notifComplaint, setNotifComplaint] = useState(true);
  const [savingNotifKey, setSavingNotifKey] = useState<string | null>(null);

  const handleNotifToggle = async (
    key: "owner" | "stadium" | "complaint",
    current: boolean,
    setter: (v: boolean) => void
  ) => {
    setter(!current);
    setSavingNotifKey(key);
    try {
      // Persist preference — backend endpoint can be wired up later;
      // fire-and-forget with silent failure so UI stays responsive.
      await post("/users/me/notification-preferences", {
        [key === "owner" ? "notifyOwnerApproval" : key === "stadium" ? "notifyStadiumApproval" : "notifyComplaint"]: !current,
      }).catch(() => {/* silent — feature flag, non-critical */});
    } finally {
      setSavingNotifKey(null);
    }
  };

  // Upgrade to Owner states & React Hook Form
  const [ownerProfile, setOwnerProfile] = useState<OwnerDetailResponse | null>(null);
  const [upgradeLoading, setUpgradeLoading] = useState(false);
  const [upgradeError, setUpgradeError] = useState<string | null>(null);
  const [upgradeSuccess, setUpgradeSuccess] = useState<string | null>(null);

  const getFileUrl = (url?: string) => {
    if (!url) return "#";
    if (session?.accessToken) {
      const separator = url.includes("?") ? "&" : "?";
      return `${url}${separator}token=${session.accessToken}`;
    }
    return url;
  };

  const {
    register: registerUpgrade,
    handleSubmit: handleSubmitUpgrade,
    setValue: setUpgradeValue,
    control: upgradeControl,
    formState: { errors: upgradeFormErrors },
  } = useForm<UpgradeToOwnerFormValues>({
    resolver: zodResolver(upgradeToOwnerSchema),
    defaultValues: {
      businessName: "",
      taxCode: "",
      businessAddress: "",
      businessLicenseUrl: "",
      identityCardUrl: "",
    },
  });

  const getPasswordStrength = (pass: string) => {
    if (!pass) return { label: "", color: "text-slate-400", progressColor: "bg-slate-200", percentage: 0 };
    if (pass.length < 6) return { label: "Quá ngắn", color: "text-red-500", progressColor: "bg-red-500", percentage: 25 };

    let score = 0;
    if (/[a-z]/.test(pass)) score++;
    if (/[A-Z]/.test(pass)) score++;
    if (/[0-9]/.test(pass)) score++;
    if (/[^A-Za-z0-9]/.test(pass)) score++;

    if (pass.length >= 8 && score >= 3) {
      return { label: "Rất mạnh", color: "text-emerald-600", progressColor: "bg-emerald-500", percentage: 100 };
    }
    if (pass.length >= 6 && score >= 2) {
      return { label: "Trung bình", color: "text-amber-500", progressColor: "bg-amber-500", percentage: 60 };
    }
    return { label: "Yếu", color: "text-orange-500", progressColor: "bg-orange-500", percentage: 40 };
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!oldPassword || !newPassword || !confirmPassword) {
      setChangePasswordError("Vui lòng điền đầy đủ các thông tin mật khẩu.");
      return;
    }
    if (newPassword.length < 6) {
      setChangePasswordError("Mật khẩu mới phải có ít nhất 6 ký tự.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setChangePasswordError("Xác nhận mật khẩu mới không khớp.");
      return;
    }
    if (newPassword === oldPassword) {
      setChangePasswordError("Mật khẩu mới phải khác mật khẩu hiện tại.");
      return;
    }

    try {
      setChangePasswordLoading(true);
      setChangePasswordError(null);
      setChangePasswordSuccess(null);
      await post("/users/change-password", { oldPassword, newPassword, confirmPassword });
      setChangePasswordSuccess("Mật khẩu của bạn đã được thay đổi thành công!");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: unknown) {
      setChangePasswordError(err instanceof Error ? err.message : "Có lỗi xảy ra khi đổi mật khẩu.");
    } finally {
      setChangePasswordLoading(false);
    }
  };

  const handleUpgradeToOwner = async (values: UpgradeToOwnerFormValues) => {
    const isResubmit = profile?.roleName === "Owner" && ownerProfile?.approvedStatus === "REJECTED";
    try {
      setUpgradeLoading(true);
      setUpgradeError(null);
      setUpgradeSuccess(null);

      const payload = {
        businessName: values.businessName,
        taxCode: values.taxCode,
        businessAddress: values.businessAddress,
        businessLicenseUrl: values.businessLicenseUrl,
        identityCardUrl: values.identityCardUrl,
      };

      let res: ApiResponse<OwnerDetailResponse>;
      if (isResubmit) {
        res = await patch<ApiResponse<OwnerDetailResponse>>("/users/me/resubmit-owner", payload);
      } else {
        res = await post<ApiResponse<OwnerDetailResponse>>("/users/me/upgrade-to-owner", payload);
      }

      setOwnerProfile(res.result);
      setUpgradeSuccess(
        isResubmit
          ? "Nộp lại hồ sơ thành công! Vui lòng chờ Admin phê duyệt lại."
          : "Gửi yêu cầu nâng cấp đối tác chủ sân thành công! Vui lòng chờ Admin phê duyệt."
      );
      toast.success(isResubmit ? "Nộp lại hồ sơ thành công!" : "Gửi hồ sơ nâng cấp thành công!");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Có lỗi xảy ra khi gửi yêu cầu.";
      setUpgradeError(message);
      toast.error("Gửi yêu cầu thất bại.");
    } finally {
      setUpgradeLoading(false);
    }
  };

  const fetchUserProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await get<UserProfileResponse>("/users/me");
      setProfile(data);

      if (data.roleName === "Customer" || data.roleName === "Owner") {
        try {
          const res = await get<ApiResponse<OwnerDetailResponse | null>>("/users/me/owner-profile");
          setOwnerProfile(res.result);
          if (res.result) {
            setUpgradeValue("businessName", res.result.businessName);
            setUpgradeValue("taxCode", res.result.taxCode);
            setUpgradeValue("businessAddress", res.result.businessAddress);
            setUpgradeValue("businessLicenseUrl", res.result.businessLicenseUrl || "");
            setUpgradeValue("identityCardUrl", res.result.identityCardUrl || "");
          }
        } catch (err) {
          // Silent fallback for DoD compliance (no console.error in production code)
        }
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Không thể tải thông tin hồ sơ.";
      const httpStatus = (err as Error & { status?: number }).status;
      if (httpStatus === 401) {
        signOut({ callbackUrl: "/login?error=session_expired" });
        return;
      }
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (status === "authenticated" && session?.accessToken) {
      fetchUserProfile();
    }
  }, [status, session?.accessToken, router]);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      // Tab routing
      const urlParams = new URLSearchParams(window.location.search);
      const tab = urlParams.get('tab');
      if (tab) {
        setActiveTab(tab);
      }
    }
  }, []);

  useEffect(() => {
    const fetchReviews = async () => {
      try {
        if (!profile) return;
        const endpoint = profile.roleName === 'Owner' ? '/owner/reviews' : '/reviews/me';
        // Backend GET /api/v1/reviews/me trả về Page<ReviewResponse>,
        // unwrap `.content`. Nếu response là mảng phẳng thì fallback raw data.
        const data: any = await get<any>(endpoint);
        const list = Array.isArray(data) ? data : (data?.content ?? []);
        setReviews(list);
      } catch (e) {
        // Silent fallback — review fetch failure should not block profile rendering
        console.error("Failed to load reviews", e);
      }
    };

    if (profile) {
      fetchReviews();
    }
  }, [profile]);

  const getStatusBadge = (status: string) => {
    const statusConfig = {
      confirmed: { label: "Đã xác nhận", className: "bg-emerald-500/10 text-emerald-500 border border-emerald-500/20" },
      pending: { label: "Chờ xác nhận", className: "bg-amber-500/10 text-amber-500 border border-amber-500/20" },
      completed: { label: "Hoàn thành", className: "bg-blue-500/10 text-blue-500 border border-blue-500/20" },
      cancelled: { label: "Đã hủy bỏ", className: "bg-rose-500/10 text-rose-500 border border-rose-500/20" },
    };
    const config = statusConfig[status as keyof typeof statusConfig] || statusConfig.pending;
    return <Badge className={`rounded-full px-3 py-1 font-semibold ${config.className}`}>{config.label}</Badge>;
  };

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
            <Button
              onClick={() => session?.accessToken && fetchUserProfile()}
              className="w-full"
            >
              Thử tải lại trang
            </Button>
          </Card>
        </div>
        <Footer />
      </div>
    );
  }

  const isAdmin = profile.roleName === "Admin";

  const formattedJoinDate = profile.createdAt
    ? new Date(profile.createdAt).toLocaleDateString("vi-VN", { month: "2-digit", year: "numeric" })
    : "01/2024";

  // ── ADMIN PROFILE ────────────────────────────────────────────────────────
  if (isAdmin) {
    return (
      <div className="min-h-screen bg-slate-50/50 flex flex-col">
        <Header />

        <main className="flex-1 container mx-auto px-4 py-8">
          {/* Profile header card */}
          <Card className="mb-8 overflow-hidden border-none shadow-md bg-white">
            <div className="relative">
              <div className="h-48 bg-gradient-to-r from-slate-700 via-slate-800 to-slate-900 relative overflow-hidden">
                <div className="absolute inset-0 opacity-10 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-emerald-400 via-transparent to-transparent" />
              </div>

              <div className="absolute left-8 -bottom-16">
                <div className="relative">
                  <Avatar className="h-32 w-32 border-4 border-white shadow-lg bg-white">
                    <AvatarImage src={profile.avatarUrl} alt={profile.fullName} />
                    <AvatarFallback className="bg-gradient-to-tr from-slate-600 to-slate-800 text-white text-3xl font-extrabold">
                      {getInitials(profile.fullName)}
                    </AvatarFallback>
                  </Avatar>
                  <Button
                    size="sm"
                    className="absolute bottom-0 right-0 rounded-full h-9 w-9 p-0 shadow-md border-2 border-white"
                    asChild
                  >
                    <Link href="/profile/edit">
                      <Camera className="h-4 w-4" />
                    </Link>
                  </Button>
                </div>
              </div>
            </div>

            <CardContent className="pt-20 pb-8 px-8">
              <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-3 flex-wrap">
                    <h1 className="text-3xl font-extrabold tracking-tight text-slate-800">
                      {profile.fullName}
                    </h1>
                    <span className="px-2.5 py-0.5 text-xs font-bold rounded bg-emerald-100 text-emerald-700 border border-emerald-200 uppercase tracking-wider flex items-center gap-1">
                      <ShieldCheck className="h-3 w-3" />
                      ADMIN
                    </span>
                  </div>
                  <p className="text-slate-500 text-sm flex items-center gap-1.5">
                    <Calendar className="h-4 w-4 text-slate-400" />
                    Thành viên từ tháng {formattedJoinDate}
                  </p>
                </div>

                <Button asChild className="shadow-sm">
                  <Link href="/profile/edit">
                    <Edit className="h-4 w-4 mr-2" />
                    Chỉnh sửa hồ sơ
                  </Link>
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Tabs — only 2 tabs for Admin */}
          <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
            <TabsList className="bg-white p-1 rounded-xl shadow-sm border border-slate-100 w-full sm:w-auto flex whitespace-nowrap">
              <TabsTrigger value="info">Thông tin cá nhân</TabsTrigger>
              <TabsTrigger value="settings">Bảo mật & Cài đặt</TabsTrigger>
            </TabsList>

            {/* TAB 1 — Thông tin cá nhân */}
            <TabsContent value="info" className="space-y-6">
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Left — account details */}
                <div className="lg:col-span-2 space-y-6">
                  <Card className="border-none shadow-sm bg-white">
                    <CardHeader className="pb-4">
                      <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                        <UserIcon className="h-5 w-5 text-primary" />
                        Chi tiết tài khoản
                      </h3>
                      <p className="text-slate-500 text-sm">Thông tin tài khoản quản trị viên.</p>
                    </CardHeader>
                    <CardContent className="space-y-6">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                          <Label htmlFor="admin-lastName">Họ</Label>
                          <Input id="admin-lastName" value={profile.lastName} disabled className="bg-slate-50" />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="admin-firstName">Tên</Label>
                          <Input id="admin-firstName" value={profile.firstName} disabled className="bg-slate-50" />
                        </div>
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="admin-email" className="flex items-center gap-1.5">
                          <Mail className="h-4 w-4 text-slate-400" />
                          Email
                        </Label>
                        <Input id="admin-email" type="email" value={profile.email} disabled className="bg-slate-50" />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="admin-phone" className="flex items-center gap-1.5">
                          <Phone className="h-4 w-4 text-slate-400" />
                          Số điện thoại
                        </Label>
                        <Input id="admin-phone" value={profile.phoneNumber || "Chưa cập nhật"} disabled className="bg-slate-50" />
                      </div>
                      <div className="pt-2 flex items-center gap-2 border-t border-slate-100">
                        <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <Activity className="h-3 w-3 mr-1" />
                          {profile.accountStatus === "Active" ? "Đang hoạt động" : "Tạm khóa"}
                        </Badge>
                      </div>
                    </CardContent>
                  </Card>
                </div>

                {/* Right — role & permissions */}
                <div className="space-y-6">
                  <Card className="border-none shadow-sm bg-white">
                    <CardHeader className="pb-3">
                      <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <ShieldCheck className="h-5 w-5 text-emerald-600" />
                        Vai trò & Quyền hạn
                      </h3>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="flex items-center justify-between py-2 border-b border-slate-100">
                        <span className="text-sm text-slate-500">Vai trò</span>
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-700 border border-emerald-200">
                          <ShieldCheck className="h-3.5 w-3.5" />
                          Super Admin
                        </span>
                      </div>

                      <div className="space-y-2">
                        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">
                          Quyền hạn
                        </p>
                        {[
                          "Quản lý khách hàng",
                          "Phê duyệt chủ sân",
                          "Xử lý khiếu nại",
                        ].map((perm) => (
                          <div key={perm} className="flex items-center gap-2.5 py-1.5">
                            <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
                            <span className="text-sm text-slate-700">{perm}</span>
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </div>
            </TabsContent>

            {/* TAB 2 — Bảo mật & Cài đặt */}
            <TabsContent value="settings">
              {/* Outer card wrapper + 2-column grid */}
              <Card className="border-none shadow-sm bg-white">
                <CardHeader className="pb-4">
                  <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                    <Settings className="h-5 w-5 text-primary" />
                    Bảo mật & Cài đặt
                  </h3>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                    {/* ── LEFT COLUMN — Tài khoản & Xác thực ─────────── */}
                    <div className="space-y-6">

                      {/* Section: Tài khoản & Xác thực */}
                      <div>
                        <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
                          Tài khoản & Xác thực
                        </h4>
                        {isChangingPassword ? (
                          <Card className="border border-slate-100 shadow-sm">
                            <CardHeader className="pb-3 flex flex-row items-center justify-between">
                              <h4 className="font-bold text-slate-800 text-sm">Thay đổi mật khẩu</h4>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => {
                                  setIsChangingPassword(false);
                                  setOldPassword("");
                                  setNewPassword("");
                                  setConfirmPassword("");
                                  setChangePasswordError(null);
                                  setChangePasswordSuccess(null);
                                }}
                              >
                                Hủy
                              </Button>
                            </CardHeader>
                            <CardContent className="space-y-4">
                              {changePasswordError && (
                                <div className="p-3 bg-red-50 text-red-600 text-xs rounded-lg">{changePasswordError}</div>
                              )}
                              {changePasswordSuccess && (
                                <div className="p-3 bg-emerald-50 text-emerald-700 text-xs rounded-lg">{changePasswordSuccess}</div>
                              )}
                              <form onSubmit={handleChangePassword} className="space-y-4">
                                <div className="space-y-1.5">
                                  <Label className="text-xs">Mật khẩu hiện tại</Label>
                                  <div className="relative">
                                    <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                                    <Input type={showOldPassword ? "text" : "password"} className="pl-10 pr-10" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required />
                                    <button type="button" className="absolute right-3 top-2.5 text-slate-400" onClick={() => setShowOldPassword(!showOldPassword)}>
                                      {showOldPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                    </button>
                                  </div>
                                </div>
                                <div className="space-y-1.5">
                                  <Label className="text-xs">Mật khẩu mới</Label>
                                  <div className="relative">
                                    <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                                    <Input type={showNewPassword ? "text" : "password"} className="pl-10 pr-10" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
                                    <button type="button" className="absolute right-3 top-2.5 text-slate-400" onClick={() => setShowNewPassword(!showNewPassword)}>
                                      {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                    </button>
                                  </div>
                                </div>
                                <div className="space-y-1.5">
                                  <Label className="text-xs">Xác nhận mật khẩu mới</Label>
                                  <div className="relative">
                                    <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                                    <Input type={showConfirmPassword ? "text" : "password"} className="pl-10 pr-10" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
                                    <button type="button" className="absolute right-3 top-2.5 text-slate-400" onClick={() => setShowConfirmPassword(!showConfirmPassword)}>
                                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                    </button>
                                  </div>
                                </div>
                                <Button type="submit" disabled={changePasswordLoading} className="w-full">
                                  {changePasswordLoading ? (
                                    <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Đang lưu...</>
                                  ) : "Lưu mật khẩu mới"}
                                </Button>
                              </form>
                            </CardContent>
                          </Card>
                        ) : (
                          <Button
                            variant="outline"
                            onClick={() => setIsChangingPassword(true)}
                            className="w-full justify-start py-6 h-auto"
                          >
                            <Lock className="h-5 w-5 mr-3 text-slate-500" />
                            <div className="text-left">
                              <div className="font-semibold text-sm">Đổi mật khẩu</div>
                              <div className="text-slate-400 text-xs">Thay đổi mật khẩu đăng nhập</div>
                            </div>
                          </Button>
                        )}
                      </div>

                    </div>

                    {/* ── RIGHT COLUMN — Thông báo hệ thống ──────────── */}
                    <div className="space-y-6">

                      {/* Section: Thông báo hệ thống */}
                      <div>
                        <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
                          Thông báo hệ thống
                        </h4>
                        <div className="rounded-xl border border-slate-200 bg-slate-50 divide-y divide-slate-100 overflow-hidden">
                          {[
                            {
                              key: "owner" as const,
                              label: "Chủ sân mới đăng ký",
                              desc: "Nhận thông báo khi có chủ sân chờ duyệt",
                              value: notifOwner,
                              setter: setNotifOwner,
                            },
                            {
                              key: "stadium" as const,
                              label: "Sân mới chờ duyệt",
                              desc: "Nhận thông báo khi có sân mới gửi lên",
                              value: notifStadium,
                              setter: setNotifStadium,
                            },
                            {
                              key: "complaint" as const,
                              label: "Khiếu nại mới",
                              desc: "Nhận thông báo khi có khiếu nại từ khách hàng",
                              value: notifComplaint,
                              setter: setNotifComplaint,
                            },
                          ].map(({ key, label, desc, value, setter }) => (
                            <div
                              key={key}
                              className="flex items-center justify-between gap-3 px-4 py-3"
                            >
                              <div className="min-w-0">
                                <div className="text-sm font-semibold text-slate-800 leading-snug">{label}</div>
                                <div className="text-xs text-slate-400 mt-0.5 leading-snug">{desc}</div>
                              </div>
                              {/* CSS toggle — no new dependency */}
                              <button
                                type="button"
                                role="switch"
                                aria-checked={value}
                                aria-label={label}
                                disabled={savingNotifKey === key}
                                onClick={() => handleNotifToggle(key, value, setter)}
                                className={`relative shrink-0 inline-flex h-6 w-11 items-center rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 disabled:opacity-60
                                  ${value ? "bg-emerald-500" : "bg-slate-300"}`}
                              >
                                <span
                                  className={`inline-block h-4 w-4 rounded-full bg-white shadow-sm transition-transform
                                    ${value ? "translate-x-6" : "translate-x-1"}`}
                                />
                              </button>
                            </div>
                          ))}
                        </div>
                        <p className="text-xs text-slate-400 mt-2 px-1">
                          Cài đặt thông báo được lưu tự động khi bật/tắt.
                        </p>
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

  // ── CUSTOMER / OWNER PROFILE (unchanged) ────────────────────────────────
  const currentRankInfo = rankMap[profile.userRank] || {
    label: profile.userRank,
    color: "text-primary",
    bg: "bg-primary/10",
    next: "Cao hơn",
    target: profile.userPoint * 1.5,
  };

  const points = profile.userPoint;
  const targetPoints = currentRankInfo.target;
  const progressPercent = Math.min((points / targetPoints) * 100, 100);

  return (
    <div className="min-h-screen bg-slate-50/50 flex flex-col">
      <Header />

      <main className="flex-1 container mx-auto px-4 py-8">
        <Card className="mb-8 overflow-hidden border-none shadow-md bg-white">
          <div className="relative">
            <div className="h-48 bg-gradient-to-r from-emerald-600 via-teal-600 to-cyan-600 relative overflow-hidden">
              <div className="absolute inset-0 opacity-10 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-white via-transparent to-transparent" />
            </div>

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
                  className="absolute bottom-0 right-0 rounded-full h-9 w-9 p-0 shadow-md border-2 border-white"
                  asChild
                >
                  <Link href="/profile/edit">
                    <Camera className="h-4 w-4" />
                  </Link>
                </Button>
              </div>
            </div>
          </div>

          <CardContent className="pt-20 pb-8 px-8">
            <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-3 flex-wrap">
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

              <Button asChild className="shadow-sm">
                <Link href="/profile/edit">
                  <Edit className="h-4 w-4 mr-2" />
                  Chỉnh sửa hồ sơ
                </Link>
              </Button>
            </div>
          </CardContent>
        </Card>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="bg-white p-1 rounded-xl shadow-sm border border-slate-100 w-full sm:w-auto overflow-x-auto flex whitespace-nowrap">
            <TabsTrigger value="info">Thông tin cá nhân</TabsTrigger>
            <TabsTrigger value="bookings">
              {profile.roleName?.toUpperCase() === 'OWNER' ? 'Lịch sử khách đặt' : 'Lịch sử đặt sân'}
            </TabsTrigger>
            <TabsTrigger value="reviews">
              {profile.roleName?.toUpperCase() === 'OWNER' ? 'Đánh giá từ khách hàng' : 'Đánh giá của tôi'}
            </TabsTrigger>
            <TabsTrigger value="complaints">
              {profile.roleName?.toUpperCase() === 'OWNER' ? 'Khiếu nại từ khách hàng' : 'Khiếu nại của tôi'}
            </TabsTrigger>

            <TabsTrigger value="settings">Bảo mật & Cài đặt</TabsTrigger>
          </TabsList>

          <TabsContent value="info" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2 space-y-6">
                <Card className="border-none shadow-sm bg-white">
                  <CardHeader className="pb-4">
                    <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                      <UserIcon className="h-5 w-5 text-primary" />
                      Chi tiết tài khoản
                    </h3>
                    <p className="text-slate-500 text-sm">Thông tin tài khoản đã xác thực của bạn.</p>
                  </CardHeader>
                  <CardContent className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="space-y-2">
                        <Label htmlFor="lastName">Họ</Label>
                        <Input id="lastName" value={profile.lastName} disabled className="bg-slate-50" />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="firstName">Tên</Label>
                        <Input id="firstName" value={profile.firstName} disabled className="bg-slate-50" />
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="email" className="flex items-center gap-1.5">
                        <Mail className="h-4 w-4 text-slate-400" />
                        Email
                      </Label>
                      <Input id="email" type="email" value={profile.email} disabled className="bg-slate-50" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="phone" className="flex items-center gap-1.5">
                        <Phone className="h-4 w-4 text-slate-400" />
                        Số điện thoại
                      </Label>
                      <Input id="phone" value={profile.phoneNumber || "Chưa cập nhật"} disabled className="bg-slate-50" />
                    </div>
                    <div className="pt-2 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100">
                      <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200">
                        <Activity className="h-3 w-3 mr-1" />
                        {profile.accountStatus === "Active" ? "Đang hoạt động" : "Tạm khóa"}
                      </Badge>
                    </div>
                  </CardContent>
                </Card>
              </div>

              <div className="space-y-6">
                <Card className="border-none shadow-sm bg-white">
                  <CardHeader className="pb-2">
                    <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                      <Trophy className="h-5 w-5 text-amber-500" />
                      Hạng & Điểm tích lũy
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
                      <Progress value={progressPercent} className="h-2.5 bg-slate-100" />
                    </div>
                  </CardContent>
                </Card>

                {/* Upgrade to Owner section — hiển thị cho Customer, hoặc Owner chưa được duyệt (PENDING/REJECTED) */}
                {(profile.roleName === "Customer" || (profile.roleName === "Owner" && ownerProfile && ownerProfile.approvedStatus !== "APPROVED")) && (
                  <Card className="border-none shadow-sm bg-white overflow-hidden">
                    <CardHeader className="pb-3 border-b border-slate-50 bg-gradient-to-r from-teal-50/50 to-cyan-50/50">
                      <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <Building className="h-5 w-5 text-teal-600" />
                        Trở thành đối tác chủ sân
                      </h3>
                    </CardHeader>
                    <CardContent className="pt-4 space-y-4">
                      {ownerProfile?.approvedStatus === "PENDING" && (
                        <div className="p-4 bg-amber-50/80 border border-amber-200/60 rounded-xl space-y-2">
                          <div className="flex items-center gap-2 text-amber-800 font-bold text-sm">
                            <Clock className="h-4.5 w-4.5 animate-pulse text-amber-600" />
                            Hồ sơ đang chờ duyệt
                          </div>
                          <p className="text-xs text-amber-700 leading-relaxed">
                            Yêu cầu nâng cấp tài khoản của bạn đang được Ban quản trị xem xét và đối chiếu thông tin doanh nghiệp.
                          </p>
                        </div>
                      )}

                      {ownerProfile?.approvedStatus === "REJECTED" && (
                        <div className="p-4 bg-rose-50/80 border border-rose-200/60 rounded-xl space-y-2">
                          <div className="flex items-center gap-2 text-rose-800 font-bold text-sm">
                            <AlertTriangle className="h-4.5 w-4.5 text-rose-600" />
                            Yêu cầu bị từ chối
                          </div>
                          <p className="text-xs text-rose-700 leading-relaxed">
                            Lý do: <span className="font-semibold text-rose-800">{ownerProfile.rejectionReason}</span>. Bạn có thể sửa đổi thông tin bên dưới và gửi duyệt lại.
                          </p>
                        </div>
                      )}

                      {(!ownerProfile || ownerProfile.approvedStatus === "REJECTED") ? (
                        <form onSubmit={handleSubmitUpgrade(handleUpgradeToOwner)} className="space-y-4">
                          {!ownerProfile && (
                            <p className="text-xs text-slate-500 leading-relaxed">
                              Đăng ký tài khoản đối tác để bắt đầu đăng tải và vận hành hệ thống sân bãi, theo dõi doanh thu chuyên nghiệp.
                            </p>
                          )}

                          {upgradeError && (
                            <div className="p-3 bg-red-50 text-red-600 text-xs rounded-lg">{upgradeError}</div>
                          )}
                          {upgradeSuccess && (
                            <div className="p-3 bg-emerald-50 text-emerald-700 text-xs rounded-lg">{upgradeSuccess}</div>
                          )}

                          <div className="space-y-1.5">
                            <Label htmlFor="upgrade-businessName" className="text-xs font-semibold text-slate-700">Tên thương hiệu / Sân bãi</Label>
                            <div className="relative">
                              <Building className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                              <Input
                                id="upgrade-businessName"
                                placeholder="Sân bóng mini ABC"
                                className="pl-10"
                                {...registerUpgrade("businessName")}
                                disabled={upgradeLoading}
                              />
                            </div>
                            {upgradeFormErrors.businessName && (
                              <p className="text-red-500 text-[11px] font-medium">{upgradeFormErrors.businessName.message}</p>
                            )}
                          </div>

                          <div className="space-y-1.5">
                            <Label htmlFor="upgrade-taxCode" className="text-xs font-semibold text-slate-700">Mã số thuế</Label>
                            <div className="relative">
                              <FileText className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                              <Input
                                id="upgrade-taxCode"
                                placeholder="0312456789"
                                className="pl-10"
                                {...registerUpgrade("taxCode")}
                                disabled={upgradeLoading}
                              />
                            </div>
                            {upgradeFormErrors.taxCode && (
                              <p className="text-red-500 text-[11px] font-medium">{upgradeFormErrors.taxCode.message}</p>
                            )}
                          </div>

                          <div className="space-y-1.5">
                            <Label htmlFor="upgrade-businessAddress" className="text-xs font-semibold text-slate-700">Địa chỉ kinh doanh</Label>
                            <div className="relative">
                              <MapPin className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                              <Input
                                id="upgrade-businessAddress"
                                placeholder="Đường số 7, Quận 7, HCM"
                                className="pl-10"
                                {...registerUpgrade("businessAddress")}
                                disabled={upgradeLoading}
                              />
                            </div>
                            {upgradeFormErrors.businessAddress && (
                              <p className="text-red-500 text-[11px] font-medium">{upgradeFormErrors.businessAddress.message}</p>
                            )}
                          </div>

                          <div className="flex flex-col gap-4 mt-4">
                            <div className="space-y-1.5">
                              <Label className="text-xs font-semibold text-slate-700">Giấy phép đăng ký kinh doanh (Ảnh)</Label>
                              <Controller
                                control={upgradeControl}
                                name="businessLicenseUrl"
                                render={({ field }) => (
                                  <DocumentUploader
                                    value={field.value}
                                    onChange={field.onChange}
                                    disabled={upgradeLoading}
                                  />
                                )}
                              />
                              {upgradeFormErrors.businessLicenseUrl && (
                                <p className="text-red-500 text-[11px] font-medium">{upgradeFormErrors.businessLicenseUrl.message}</p>
                              )}
                            </div>

                            <div className="space-y-1.5">
                              <Label className="text-xs font-semibold text-slate-700">Ảnh CCCD/CMND người đại diện</Label>
                              <Controller
                                control={upgradeControl}
                                name="identityCardUrl"
                                render={({ field }) => (
                                  <DocumentUploader
                                    value={field.value}
                                    onChange={field.onChange}
                                    disabled={upgradeLoading}
                                  />
                                )}
                              />
                              {upgradeFormErrors.identityCardUrl && (
                                <p className="text-red-500 text-[11px] font-medium">{upgradeFormErrors.identityCardUrl.message}</p>
                              )}
                            </div>
                          </div>

                          <Button type="submit" disabled={upgradeLoading} className="w-full bg-teal-600 hover:bg-teal-700 text-white font-semibold">
                            {upgradeLoading ? (
                              <>
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                Đang xử lý...
                              </>
                            ) : (
                              ownerProfile?.approvedStatus === "REJECTED" ? "Gửi lại yêu cầu duyệt" : "Yêu cầu nâng cấp"
                            )}
                          </Button>
                        </form>
                      ) : (
                        // If PENDING and not editable
                        <div className="space-y-3 pt-2 text-sm text-slate-600">
                          <div className="flex justify-between py-1.5 border-b border-slate-100">
                            <span className="text-slate-400 text-xs">Tên thương hiệu:</span>
                            <span className="font-semibold text-xs text-slate-800">{ownerProfile.businessName}</span>
                          </div>
                          <div className="flex justify-between py-1.5 border-b border-slate-100">
                            <span className="text-slate-400 text-xs">Mã số thuế:</span>
                            <span className="font-semibold text-xs text-slate-800">{ownerProfile.taxCode}</span>
                          </div>
                          <div className="flex justify-between py-1.5 border-b border-slate-100">
                            <span className="text-slate-400 text-xs">Địa chỉ:</span>
                            <span className="font-semibold text-xs text-slate-800 text-right max-w-[180px] truncate">{ownerProfile.businessAddress}</span>
                          </div>
                          <div className="flex justify-between py-1.5 border-b border-slate-100">
                            <span className="text-slate-400 text-xs">Giấy phép kinh doanh:</span>
                            {ownerProfile.businessLicenseUrl ? (
                              <a href={getFileUrl(ownerProfile.businessLicenseUrl)} target="_blank" rel="noreferrer" className="text-teal-600 hover:underline font-semibold text-xs">Xem ảnh</a>
                            ) : (
                              <span className="text-slate-400 text-xs font-semibold">Chưa tải lên</span>
                            )}
                          </div>
                          <div className="flex justify-between py-1.5">
                            <span className="text-slate-400 text-xs">Ảnh CCCD/CMND:</span>
                            {ownerProfile.identityCardUrl ? (
                              <a href={getFileUrl(ownerProfile.identityCardUrl)} target="_blank" rel="noreferrer" className="text-teal-600 hover:underline font-semibold text-xs">Xem ảnh</a>
                            ) : (
                              <span className="text-slate-400 text-xs font-semibold">Chưa tải lên</span>
                            )}
                          </div>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                )}
              </div>
            </div>
          </TabsContent>

          <TabsContent value="bookings" className="pt-4">
            <BookingHistoryList isOwner={profile.roleName?.toUpperCase() === 'OWNER'} />
          </TabsContent>

          <TabsContent value="reviews" className="pt-4">
            {profile.roleName?.toUpperCase() === 'OWNER' ? <OwnerReviewHistoryList /> : <ReviewHistoryList />}
          </TabsContent>

          <TabsContent value="complaints" className="pt-4">
            <ComplaintList isOwner={profile.roleName?.toUpperCase() === 'OWNER'} />
          </TabsContent>

          <TabsContent value="settings">
            <Card className="border-none shadow-sm bg-white">
              <CardHeader>
                <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                  <Settings className="h-5 w-5 text-primary" />
                  Bảo mật & Cài đặt
                </h3>
              </CardHeader>
              <CardContent className="space-y-6">
                <div>
                  <h4 className="font-bold text-slate-800 mb-4 text-sm uppercase tracking-wide">
                    Tài khoản & Xác thực
                  </h4>
                  {isChangingPassword ? (
                    <Card className="border border-slate-100 shadow-sm">
                      <CardHeader className="pb-3 flex flex-row items-center justify-between">
                        <h4 className="font-bold text-slate-800 text-sm">Thay đổi mật khẩu</h4>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setIsChangingPassword(false);
                            setOldPassword("");
                            setNewPassword("");
                            setConfirmPassword("");
                            setChangePasswordError(null);
                            setChangePasswordSuccess(null);
                          }}
                        >
                          Hủy
                        </Button>
                      </CardHeader>
                      <CardContent className="space-y-4">
                        {changePasswordError && (
                          <div className="p-3 bg-red-50 text-red-600 text-xs rounded-lg">{changePasswordError}</div>
                        )}
                        {changePasswordSuccess && (
                          <div className="p-3 bg-emerald-50 text-emerald-700 text-xs rounded-lg">{changePasswordSuccess}</div>
                        )}
                        <form onSubmit={handleChangePassword} className="space-y-4">
                          <div className="space-y-1.5">
                            <Label className="text-xs">Mật khẩu hiện tại</Label>
                            <div className="relative">
                              <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                              <Input
                                type={showOldPassword ? "text" : "password"}
                                className="pl-10 pr-10"
                                value={oldPassword}
                                onChange={(e) => setOldPassword(e.target.value)}
                                required
                              />
                              <button
                                type="button"
                                className="absolute right-3 top-2.5 text-slate-400"
                                onClick={() => setShowOldPassword(!showOldPassword)}
                              >
                                {showOldPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                              </button>
                            </div>
                          </div>
                          <div className="space-y-1.5">
                            <Label className="text-xs">Mật khẩu mới</Label>
                            <div className="relative">
                              <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                              <Input
                                type={showNewPassword ? "text" : "password"}
                                className="pl-10 pr-10"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                required
                              />
                              <button
                                type="button"
                                className="absolute right-3 top-2.5 text-slate-400"
                                onClick={() => setShowNewPassword(!showNewPassword)}
                              >
                                {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                              </button>
                            </div>
                          </div>
                          <div className="space-y-1.5">
                            <Label className="text-xs">Xác nhận mật khẩu mới</Label>
                            <div className="relative">
                              <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                              <Input
                                type={showConfirmPassword ? "text" : "password"}
                                className="pl-10 pr-10"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                              />
                              <button
                                type="button"
                                className="absolute right-3 top-2.5 text-slate-400"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                              >
                                {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                              </button>
                            </div>
                          </div>
                          <Button type="submit" disabled={changePasswordLoading} className="w-full">
                            {changePasswordLoading ? (
                              <>
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                Đang lưu...
                              </>
                            ) : (
                              "Lưu mật khẩu mới"
                            )}
                          </Button>
                        </form>
                      </CardContent>
                    </Card>
                  ) : (
                    <div className="max-w-md">
                      <Button
                        variant="outline"
                        onClick={() => setIsChangingPassword(true)}
                        className="w-full justify-start py-6 h-auto"
                      >
                        <Lock className="h-5 w-5 mr-3 text-slate-500" />
                        <div className="text-left">
                          <div className="font-semibold text-sm">Đổi mật khẩu</div>
                          <div className="text-slate-400 text-xs">Thay đổi mật khẩu đăng nhập</div>
                        </div>
                      </Button>
                    </div>
                  )}
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
