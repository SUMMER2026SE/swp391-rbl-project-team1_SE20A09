"use client";

import { SessionProvider, useSession } from "next-auth/react";
import React, { useEffect } from "react";

function TokenSync() {
  const { data: session, status } = useSession();

  useEffect(() => {
    if (status === "authenticated" && session?.accessToken) {
      localStorage.setItem("access_token", session.accessToken);
    } else if (status === "unauthenticated") {
      localStorage.removeItem("access_token");
    }
  }, [session, status]);

  return null;
}

export default function NextAuthProvider({ children }: { children: React.ReactNode }) {
  return (
    <SessionProvider>
      <TokenSync />
      {children}
    </SessionProvider>
  );
}
