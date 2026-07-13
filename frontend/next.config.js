/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',       // Tối ưu cho Docker deployment
  // react-leaflet@4.2.1's MapContainer initializes the Leaflet map inside a ref
  // callback with no guard against React Strict Mode's double-invoke, so it
  // deterministically throws "Map container is already initialized" on every
  // mount in dev (App Router enables Strict Mode by default when this option
  // isn't set). Dev-only flag — no effect on production builds.
  reactStrictMode: false,
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '9000',          // MinIO dev
      },
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '8080',          // Avatar uploads từ backend
      },
      {
        protocol: 'https',
        hostname: '*.amazonaws.com',   // AWS S3 production
      },
      {
        protocol: 'https',
        hostname: 'images.unsplash.com',
      },
    ],
  },
  async rewrites() {
    return [
      {
        source: '/api/v1/:path*',
        // FIX: Dùng API_URL (không có NEXT_PUBLIC prefix) cho server-side proxy
        // Trong Docker: API_URL = http://backend:8080 (internal network)
        // Trong dev local: fallback về NEXT_PUBLIC_API_URL hoặc localhost:8080
        // Browser luôn gọi /api/v1/... → Next.js server proxy → backend
        destination: `${process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/:path*`,
      },
    ]
  },
}

module.exports = nextConfig
