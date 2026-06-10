"use client";

import { useSession } from "next-auth/react";
import { Loader2 } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { GuestHomePage } from "@/components/home/guest/GuestHomePage";
import { AuthenticatedHomePage } from "@/components/home/authenticated/AuthenticatedHomePage";

export default function HomePage() {
  const { data: session, status } = useSession();

  if (status === "loading") {
    return (
      <div className="flex min-h-screen flex-col bg-background">
        <Header />
        <div className="flex flex-1 flex-col items-center justify-center gap-3">
          <Loader2 className="h-10 w-10 animate-spin text-green-800" />
          <p className="text-sm text-muted-foreground">Đang tải trang chủ...</p>
        </div>
      </div>
    );
  }

  if (status === "authenticated" && session?.user) {
    return <AuthenticatedHomePage user={session.user} />;
  }

  return <GuestHomePage />;
}
