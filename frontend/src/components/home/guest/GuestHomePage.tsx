"use client";

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { HowItWorks } from "@/components/landing/HowItWorks";
import { HeroSection } from "@/components/landing/home/HeroSection";
import { FeaturedVenuesSection } from "@/components/landing/home/FeaturedVenuesSection";
import { PlatformHighlights } from "@/components/landing/home/PlatformHighlights";
import { HomeCtaBanner } from "@/components/landing/home/HomeCtaBanner";

export function GuestHomePage() {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <main>
        <HeroSection />
        <FeaturedVenuesSection />
        <PlatformHighlights />
        <HowItWorks />
        <HomeCtaBanner />
      </main>
      <Footer />
    </div>
  );
}
