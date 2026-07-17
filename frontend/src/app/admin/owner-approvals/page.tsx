import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";
import { adminOwnerService, OwnerDetail } from "@/lib/services/admin-owner";
import AdminOwnerApprovalsClient from "./components/AdminOwnerApprovalsClient";
import { Button } from "@/components/ui/button";
import Link from "next/link";

export default async function OwnerApprovalPage() {
  const session = await getServerSession(authOptions);
  const token = session?.accessToken;
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  let initialApplications: OwnerDetail[] = [];
  let initialTotalPages = 0;

  try {
    const res = await adminOwnerService.getRegistrations('PENDING', 0, 10, { headers });
    if (res.code === 200 && res.result) {
      initialApplications = res.result.content;
      initialTotalPages = res.result.totalPages;
    }
  } catch (error) {
    console.error("Server-side getRegistrations failed:", error);
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold">Duyệt hồ sơ chủ sân</h1>
          <p className="text-muted-foreground mt-1">Xem xét hồ sơ đăng ký kinh doanh và yêu cầu nâng cấp lên Chủ sân.</p>
        </div>
        <Button variant="outline" asChild>
          <Link href="/admin/dashboard">Quay lại Dashboard</Link>
        </Button>
      </div>
      <AdminOwnerApprovalsClient
        initialApplications={initialApplications}
        initialTotalPages={initialTotalPages}
      />
    </div>
  );
}
