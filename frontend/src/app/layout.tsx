import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import "leaflet/dist/leaflet.css";
import { Toaster } from "sonner";
import NextAuthProvider from "@/components/providers/NextAuthProvider";
import QueryProvider from "@/components/providers/QueryProvider";
import { RouteGuard } from "@/components/shared/RouteGuard";
import { ChatWidget } from "@/components/ai-assistant/ChatWidget";

const inter = Inter({
  subsets: ["latin", "vietnamese"],
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "SportsBook - Hệ thống đặt sân thể thao trực tuyến",
  description: "Đặt sân bóng đá, cầu lông, tennis và nhiều môn thể thao khác một cách dễ dàng và nhanh chóng.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={`${inter.variable} font-sans antialiased`}>
        <NextAuthProvider>
          <QueryProvider>
            <RouteGuard>
              {children}
            </RouteGuard>
            <ChatWidget />
            <Toaster position="top-right" richColors />
          </QueryProvider>
        </NextAuthProvider>
      </body>
    </html>
  );
} // trigger CI
