import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";
import { stadiumService } from "@/lib/services/stadium";
import { ComplexResponse, StadiumResponse } from "@/types/stadium";
import OwnerVenuesClient from "./components/OwnerVenuesClient";

export default async function VenueManagementPage() {
  const session = await getServerSession(authOptions);
  const token = session?.accessToken;
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  let initialComplexes: ComplexResponse[] = [];
  let initialVenues: StadiumResponse[] = [];

  try {
    const [complexesRes, venuesRes] = await Promise.all([
      stadiumService.getMyComplexes({ headers }),
      stadiumService.getMyStadiums(undefined, { headers })
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
