import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";
import { stadiumService } from "@/lib/services/stadium";
import { getAmenities, Amenity } from "@/lib/api/stadium";
import { ComplexResponse } from "@/types/stadium";
import AdminComplexApprovalsClient from "./components/AdminComplexApprovalsClient";

export default async function ComplexApprovalPage() {
  const session = await getServerSession(authOptions);
  const token = session?.accessToken;
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  let initialComplexes: ComplexResponse[] = [];
  let initialAmenities: Amenity[] = [];

  try {
    const [complexesRes, amenitiesRes] = await Promise.all([
      stadiumService.getAllComplexesAdmin('PENDING', { headers }),
      getAmenities()
    ]);
    initialComplexes = complexesRes;
    initialAmenities = amenitiesRes;
  } catch (error) {
    console.error("Server-side complex approvals fetch failed:", error);
  }

  return (
    <AdminComplexApprovalsClient
      initialComplexes={initialComplexes}
      initialAmenities={initialAmenities}
    />
  );
}
