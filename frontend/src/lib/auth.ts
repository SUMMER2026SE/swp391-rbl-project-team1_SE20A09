import { NextAuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import GoogleProvider from "next-auth/providers/google";
import axios from "axios";

const BACKEND_URL = process.env.API_URL ?? "http://localhost:8080";

export const authOptions: NextAuthOptions = {
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        email: { label: "Email", type: "email" },
        password: { label: "Mật khẩu", type: "password" }
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials?.password) {
          throw new Error("Vui lòng nhập đầy đủ email và mật khẩu.");
        }

        try {
          const response = await axios.post(`${BACKEND_URL}/api/v1/auth/login`, {
            email: credentials.email,
            password: credentials.password
          });

          if (response.data) {
            return {
              id: response.data.user.userId.toString(),
              email: response.data.user.email,
              name: `${response.data.user.firstName} ${response.data.user.lastName}`,
              image: response.data.user.avatarUrl,
              accessToken: response.data.accessToken,
              user: response.data.user
            };
          }
          return null;
        } catch (error: any) {
          const errorMsg = error.response?.data?.message ?? "Tên đăng nhập hoặc mật khẩu không chính xác.";
          throw new Error(errorMsg);
        }
      }
    }),
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID ?? "",
      clientSecret: process.env.GOOGLE_CLIENT_SECRET ?? ""
    })
  ],
  callbacks: {
    async jwt({ token, user, account }) {
      if (account?.provider === "google") {
        try {
          const response = await axios.post(`${BACKEND_URL}/api/v1/auth/google`, {
            idToken: account.id_token
          });

          if (response.data) {
            token.accessToken = response.data.accessToken;
            token.user = response.data.user;
          }
        } catch (error: any) {
          console.error("Error signing in with Google to backend:", error.response?.data ?? error.message);
        }
      } else if (user) {
        token.accessToken = user.accessToken;
        token.user = user.user;
      }
      return token;
    },
    async session({ session, token }) {
      if (token) {
        session.accessToken = token.accessToken;
        session.user = token.user;
      }
      return session;
    }
  },
  pages: {
    signIn: "/login",
    error: "/login"
  },
  session: {
    strategy: "jwt",
    maxAge: 30 * 24 * 60 * 60 // 30 days
  },
  secret: process.env.NEXTAUTH_SECRET
};
