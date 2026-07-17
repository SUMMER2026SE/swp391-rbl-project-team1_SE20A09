import { withAuth } from "next-auth/middleware";
import { NextResponse } from "next/server";

export default withAuth(
  function middleware(req) {
    const token = req.nextauth.token;
    const pathname = req.nextUrl.pathname;

    // Check account status if blocked
    if (token?.user?.accountStatus === "BLOCKED" && !pathname.startsWith("/appeals")) {
      return NextResponse.redirect(new URL("/appeals", req.url));
    }

    // Role-based authorization
    if (pathname.startsWith("/admin")) {
      if (token?.user?.roleName !== "Admin") {
        return NextResponse.redirect(new URL("/", req.url));
      }
    }

    if (pathname.startsWith("/owner")) {
      const isApprovedOwner =
        token?.user?.roleName === "Owner" && token.user.ownerApprovedStatus === "APPROVED";
      if (!isApprovedOwner) {
        return NextResponse.redirect(new URL("/profile", req.url));
      }
    }

    return NextResponse.next();
  },
  {
    callbacks: {
      authorized: ({ token }) => !!token,
    },
  }
);

export const config = {
  matcher: [
    "/admin/:path*",
    "/owner/:path*",
    "/profile/:path*",
    "/booking/:path*",
    "/chat/:path*",
    "/bookings/:path*",
    "/notifications/:path*",
    "/payments/:path*",
    "/complaints/:path*",
    "/appeals/:path*",
    "/reports/:path*",
    "/ai-assistant/:path*",
  ],
};
