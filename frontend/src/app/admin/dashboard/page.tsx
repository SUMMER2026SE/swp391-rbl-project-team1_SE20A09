import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";
import api from "@/lib/api";
import AdminDashboardClient from "./components/AdminDashboardClient";

interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

export default async function AdminDashboardPage() {
  const session = await getServerSession(authOptions);
  const token = session?.accessToken;
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  let initialData = null;
  try {
    const { data } = await api.get<ApiResponse<any>>("/admin/dashboard", { headers });
    initialData = data.result;
  } catch (error) {
    console.error("Server-side admin dashboard fetch failed:", error);
  }

  if (!initialData) {
    return (
      <div className="py-20 text-center text-rose-500 font-medium">
        Đã có lỗi xảy ra khi tải dữ liệu dashboard ở Server. Vui lòng tải lại trang.
      </div>
    );
  }

  return (
    <AdminDashboardClient
      initialData={initialData}
    />
  );
}
