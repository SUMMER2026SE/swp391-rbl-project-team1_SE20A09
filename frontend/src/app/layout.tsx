import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Toaster } from "sonner";
import NextAuthProvider from "@/components/providers/NextAuthProvider";

const inter = Inter({
  subsets: ["latin", "vietnamese"],
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "SportVenue - Hệ thống đặt sân thể thao trực tuyến",
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
          {children}
          <Toaster position="top-right" richColors />
        </NextAuthProvider>
      </body>
    </html>
  );
}
