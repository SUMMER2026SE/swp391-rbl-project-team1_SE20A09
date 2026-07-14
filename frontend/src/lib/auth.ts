import { NextAuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import GoogleProvider from "next-auth/providers/google";
import axios from "axios";

const BACKEND_URL = process.env.API_URL ?? "http://localhost:8080";

// Sau bao lâu thì tự đồng bộ lại token.user (role, approvedStatus...) từ
// backend — tránh nav bị kẹt ở role/trạng thái cũ (vd sau khi Admin duyệt
// Owner) cho tới khi user tự đăng xuất/đăng nhập lại. Xem SessionProvider's
// refetchInterval (NextAuthProvider.tsx) — client cần chủ động gọi lại
// /api/auth/session định kỳ để nhánh này thực sự được kích hoạt.
const SESSION_REFRESH_INTERVAL_MS = 5 * 60 * 1000;

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
    CredentialsProvider({
      // Đăng nhập ngầm ngay sau khi verify-otp — dùng thẳng accessToken đã có
      // sẵn từ response của POST /auth/verify-otp, thay vì lưu password
      // plaintext tạm vào sessionStorage rồi gọi lại toàn bộ luồng
      // email+password login lần 2 (cách cũ, fragile nếu sessionStorage mất
      // giữa chừng — vd F5/mở tab mới). Vẫn xác thực lại accessToken qua
      // GET /auth/me (không tin thẳng dữ liệu client gửi lên) trước khi tạo
      // session, tương tự cách provider Google verify qua backend.
      id: "otp-verified",
      name: "OtpVerified",
      credentials: {
        accessToken: { label: "Access Token", type: "text" },
      },
      async authorize(credentials) {
        if (!credentials?.accessToken) {
          return null;
        }

        try {
          const response = await axios.get(`${BACKEND_URL}/api/v1/auth/me`, {
            headers: { Authorization: `Bearer ${credentials.accessToken}` },
          });

          if (response.data) {
            return {
              id: response.data.userId.toString(),
              email: response.data.email,
              name: `${response.data.firstName} ${response.data.lastName}`,
              image: response.data.avatarUrl,
              accessToken: credentials.accessToken,
              user: response.data,
            };
          }
          return null;
        } catch {
          throw new Error("Không thể xác thực phiên đăng nhập sau khi xác minh OTP.");
        }
      },
    }),
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID ?? "",
      clientSecret: process.env.GOOGLE_CLIENT_SECRET ?? "",
      authorization: {
        params: {
          prompt: "consent",
          access_type: "offline",
          response_type: "code",
        },
      },
    })
  ],
  callbacks: {
    async jwt({ token, user, account, trigger, session }) {
      if (trigger === "update" && session?.user) {
        token.user = session.user;
        token.lastRefreshedAt = Date.now();
      } else if (account?.provider === "google") {
        try {
          const response = await axios.post(`${BACKEND_URL}/api/v1/auth/google`, {
            idToken: account.id_token
          });

          if (response.data) {
            token.accessToken = response.data.accessToken;
            token.user = response.data.user;
            token.lastRefreshedAt = Date.now();
          }
          if (account.access_token) {
            token.googleAccessToken = account.access_token;
          }
        } catch (error: any) {
          console.error("Error signing in with Google to backend:", error.response?.data ?? error.message);
        }
      } else if (user) {
        token.accessToken = user.accessToken;
        token.user = user.user;
        token.lastRefreshedAt = Date.now();
      } else if (
        token.accessToken &&
        // Gọi useSession().update() KHÔNG kèm dữ liệu (vd ngay khi có thông
        // báo mới — xem NotificationBell.tsx) bỏ qua TTL, ép refresh ngay lập
        // tức thay vì chờ tới hạn 5 phút.
        (trigger === "update" || Date.now() - (token.lastRefreshedAt ?? 0) > SESSION_REFRESH_INTERVAL_MS)
      ) {
        // Đồng bộ lại role/approvedStatus mới nhất từ backend (vd Customer ->
        // Owner sau khi Admin duyệt) — không đăng xuất nếu request lỗi/token
        // hết hạn ở đây, chỉ giữ nguyên token cũ và thử lại ở lần sau.
        try {
          const response = await axios.get(`${BACKEND_URL}/api/v1/auth/me`, {
            headers: { Authorization: `Bearer ${token.accessToken}` },
          });
          if (response.data) {
            token.user = response.data;
          }
        } catch (error: any) {
          console.error("Session refresh failed, keeping existing token:", error.response?.data ?? error.message);
        }
        token.lastRefreshedAt = Date.now();
      }
      return token;
    },
    async session({ session, token }) {
      if (token.user) {
        const { firstName, lastName, email, avatarUrl } = token.user;
        const fullName = [firstName, lastName].filter(Boolean).join(" ").trim();
        session.user = {
          ...session.user,
          ...token.user,
          email: email ?? session.user?.email,
          name: fullName || session.user?.name,
          image: avatarUrl ?? session.user?.image,
        };
      }
      if (token) {
        session.accessToken = token.accessToken;
        session.googleAccessToken = token.googleAccessToken;
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
