'use client'

import { useEffect } from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";

export default function AuthRedirectPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === "loading") return;

    const roleName = session?.user?.roleName;
    if (roleName === "Admin") {
      router.replace("/admin/dashboard");
    } else if (roleName === "Owner") {
      router.replace("/owner/dashboard");
    } else {
      router.replace("/");
    }
  }, [status, session, router]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />
    </div>
  );
}
