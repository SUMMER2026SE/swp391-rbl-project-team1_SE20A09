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
    };
  }
}

declare module "next-auth/jwt" {
  interface JWT {
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
    };
  }
}
