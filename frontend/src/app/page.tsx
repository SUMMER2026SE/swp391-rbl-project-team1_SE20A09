import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";
import { GuestHomePage } from "@/components/home/guest/GuestHomePage";
import { AuthenticatedHomePage } from "@/components/home/authenticated/AuthenticatedHomePage";

export default async function HomePage() {
  const session = await getServerSession(authOptions);

  if (session?.user) {
    return <AuthenticatedHomePage user={session.user} />;
  }

  return <GuestHomePage />;
}
