import { DefaultSession } from "next-auth";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    googleAccessToken?: string;
    user?: {
      userId: number;
      email: string;
      firstName: string;
      lastName: string;
      roleName: string;
      avatarUrl?: string;
      phoneNumber: string;
      userRank: string;
      userPoint: number;
      accountStatus: string;
      lockReason?: string;
      /** Chỉ có giá trị khi roleName = "Owner" — null với Customer/Admin. */
      ownerApprovedStatus?: string | null;
    } & DefaultSession["user"];
  }

  interface User {
    accessToken?: string;
    user?: {
      userId: number;
      email: string;
      firstName: string;
      lastName: string;
      roleName: string;
      avatarUrl?: string;
      phoneNumber: string;
      userRank: string;
      userPoint: number;
      accountStatus: string;
      lockReason?: string;
      /** Chỉ có giá trị khi roleName = "Owner" — null với Customer/Admin. */
      ownerApprovedStatus?: string | null;
    };
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    googleAccessToken?: string;
    /** Epoch ms của lần cuối token.user được đồng bộ lại từ backend — dùng để
     * định kỳ refresh role/approvedStatus mới nhất (xem lib/auth.ts jwt callback). */
    lastRefreshedAt?: number;
    user?: {
      userId: number;
      email: string;
      firstName: string;
      lastName: string;
      roleName: string;
      avatarUrl?: string;
      phoneNumber: string;
      userRank: string;
      userPoint: number;
      accountStatus: string;
      lockReason?: string;
      /** Chỉ có giá trị khi roleName = "Owner" — null với Customer/Admin. */
      ownerApprovedStatus?: string | null;
    };
  }
}
