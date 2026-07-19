import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";
import { stadiumService } from "@/lib/services/stadium";
import { ComplexResponse, StadiumResponse } from "@/types/stadium";
import OwnerVenuesClient from "./components/OwnerVenuesClient";

interface PageProps {
  searchParams: {
    status?: string;
  };
}

export default async function VenueManagementPage({ searchParams }: PageProps) {
  const session = await getServerSession(authOptions);
  const token = session?.accessToken;
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  let initialComplexes: ComplexResponse[] = [];
  let initialVenues: StadiumResponse[] = [];

  const statusFilter = searchParams.status;

  try {
    const [complexesRes, venuesRes] = await Promise.all([
      stadiumService.getMyComplexes({ headers }),
      stadiumService.getMyStadiums(statusFilter ? { status: statusFilter } : undefined, { headers })
    ]);
    initialComplexes = complexesRes;
    initialVenues = venuesRes;
  } catch (error) {
    console.error("Server-side getMyComplexes / getMyStadiums failed:", error);
  }

  return (
    <OwnerVenuesClient
      initialComplexes={initialComplexes}
      initialVenues={initialVenues}
    />
  );
}
