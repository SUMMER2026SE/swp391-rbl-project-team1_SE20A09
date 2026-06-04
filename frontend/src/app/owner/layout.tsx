'use client'

import { Header } from "@/components/layout/Header";

export default function OwnerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Header />
      <div className="flex-1">
        {children}
      </div>
    </div>
  );
}
