"use client";

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { MyReportsList } from "@/components/reports/MyReportsList";

export default function MyReportsPage() {
  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Header />

      <div className="container mx-auto px-4 py-8 flex-1">
        <div className="max-w-3xl mx-auto">
          <MyReportsList />
        </div>
      </div>

      <Footer />
    </div>
  );
}
