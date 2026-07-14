"use client";

import { SessionProvider, useSession } from "next-auth/react";
import React, { useEffect, useRef } from "react";
import { get } from "@/lib/api";
import { ApiResponse } from "@/types/notification";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

function TokenSync() {
  const { data: session, status } = useSession();

  useEffect(() => {
    if (status === "authenticated" && session?.accessToken) {
      localStorage.setItem("access_token", session.accessToken);
    } else if (status === "unauthenticated") {
      localStorage.removeItem("access_token");
    }
  }, [session, status]);

  return null;
}

const NOTIFICATION_POLL_MS = 30_000;

// Ngồi toàn cục (mọi trang, mọi role) — khi có thông báo mới (vd Admin vừa
// duyệt hồ sơ Owner), ép session refresh ngay thay vì chờ tới hạn định kỳ
// 5 phút (xem lib/auth.ts jwt callback + SessionProvider refetchInterval bên
// dưới). Không dùng react-query vì component này nằm ngoài QueryProvider.
function NotificationDrivenSessionRefresh() {
  const { status, update: updateSession } = useSession();
  const previousCountRef = useRef<number | null>(null);

  useEffect(() => {
    if (status !== "authenticated") {
      previousCountRef.current = null;
      return;
    }

    let cancelled = false;
    const poll = async () => {
      try {
        const res = await get<ApiResponse<number>>("/notifications/unread-count");
        if (cancelled) return;
        const count = res.result ?? 0;
        if (previousCountRef.current !== null && count > previousCountRef.current) {
          updateSession();
        }
        previousCountRef.current = count;
      } catch {
        // Bỏ qua lỗi tạm thời — thử lại ở lần poll tiếp theo.
      }
    };

    poll();
    const interval = setInterval(poll, NOTIFICATION_POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [status, updateSession]);

  return null;
}

// Lắng nghe sự thay đổi trạng thái tài khoản thời gian thực qua WebSocket
function AccountStatusWebSocketListener() {
  const { data: session, status, update: updateSession } = useSession();
  const userId = session?.user?.userId;

  useEffect(() => {
    if (status !== "authenticated" || !userId) return;

    const rawBase = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
    const backendBase = rawBase
      .replace(/\/api\/v1\/?$/, "")
      .replace(/^wss?:\/\//, "http://");
    const sockJsUrl = `${backendBase}/ws`;

    const token = localStorage.getItem("access_token");

    const client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        client.subscribe(`/topic/user/${userId}/account-status`, (msg) => {
          try {
            const data = JSON.parse(msg.body);
            if (data && data.status) {
              updateSession();
            }
          } catch (e) {
            console.error("Failed to parse account status WS message:", e);
          }
        });
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [status, userId, updateSession]);

  return null;
}

export default function NextAuthProvider({ children }: { children: React.ReactNode }) {
  return (
    // refetchInterval (giây) khiến client định kỳ gọi lại /api/auth/session,
    // qua đó kích hoạt nhánh tự refresh role/approvedStatus trong lib/auth.ts
    // jwt callback — nếu không có prop này, nhánh đó chỉ chạy khi có điều
    // hướng trang mới hoặc focus lại cửa sổ (NextAuth default), có thể chậm
    // hơn với user đứng yên 1 trang lâu.
    <SessionProvider refetchInterval={5 * 60}>
      <TokenSync />
      <NotificationDrivenSessionRefresh />
      <AccountStatusWebSocketListener />
      {children}
    </SessionProvider>
  );
}
