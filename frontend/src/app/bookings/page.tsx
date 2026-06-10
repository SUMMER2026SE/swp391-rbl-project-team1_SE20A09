"use client";

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { BookingHistoryList } from "@/components/bookings/BookingHistoryList";

function BookingHistoryPage() {
  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <h1 className="mb-8 text-3xl font-bold">Lịch sử đặt sân</h1>
        <BookingHistoryList />
      </div>

      <Footer />
    </div>
  );
}

export default BookingHistoryPage;
