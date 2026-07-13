"use client";

import { useRef, useState, useEffect, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Bell, Building2, MapPin, AlertOctagon, CheckCheck, ExternalLink } from "lucide-react";
import { useRouter } from "next/navigation";
import api from "@/lib/api";
import type { ApiResponse } from "@/types/common";

// ── Types ────────────────────────────────────────────────────────────────────

type NotificationType = "OWNER_APPROVAL" | "STADIUM_APPROVAL" | "COMPLAINT" | "REPORT" | string;

interface AdminNotification {
  id: string;
  type: NotificationType;
  title: string;
  description: string;
  createdAt: string;
  isRead: boolean;
  notificationId: number | null;
}

interface MarkReadRequest {
  resourceId: string;
  title: string;
  description: string;
  type: NotificationType;
  notificationId: number | null;
}

// ── API ───────────────────────────────────────────────────────────────────────

const fetchNotifications = async (): Promise<AdminNotification[]> => {
  const { data } = await api.get<ApiResponse<AdminNotification[]>>("/admin/notifications");
  return data.result;
};

const fetchUnreadCount = async (): Promise<number> => {
  const { data } = await api.get<ApiResponse<number>>("/admin/notifications/unread-count");
  return data.result;
};

const markAsReadApi = async (payload: MarkReadRequest): Promise<number> => {
  const { data } = await api.patch<ApiResponse<number>>(
    "/admin/notifications/mark-as-read",
    payload
  );
  return data.result;
};

const markAllAsReadApi = async (): Promise<void> => {
  await api.patch("/admin/notifications/mark-all-as-read");
};

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatTimestamp(iso: string): string {
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return "";
    const hh = String(d.getHours()).padStart(2, "0");
    const mm = String(d.getMinutes()).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const mo = String(d.getMonth() + 1).padStart(2, "0");
    const yyyy = d.getFullYear();
    return `${hh}:${mm} - ${dd}/${mo}/${yyyy}`;
  } catch {
    return "";
  }
}

/** Route for the action button — separate from item click */
function getHref(type: NotificationType): string {
  if (type === "OWNER_APPROVAL") return "/admin/users?tab=owners&ownerTab=approvals";
  if (type === "STADIUM_APPROVAL") return "/admin/stadium-approvals";
  if (type === "APPEAL") return "/admin/appeals";
  if (type === "REPORT") return "/admin/moderation-analytics";
  if (type === "BOOKING") return "/admin/bookings";
  return "/admin/complaints";
}

function badgeLabel(count: number): string | null {
  if (count <= 0) return null;
  if (count <= 9) return String(count);
  return "9+";
}

function TypeIcon({ type }: { type: NotificationType }) {
  if (type === "OWNER_APPROVAL")
    return <Building2 className="h-4 w-4 text-indigo-500 shrink-0 mt-0.5" />;
  if (type === "STADIUM_APPROVAL")
    return <MapPin className="h-4 w-4 text-purple-500 shrink-0 mt-0.5" />;
  if (type === "REPORT")
    return <AlertOctagon className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />;
  return <AlertOctagon className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />;
}

// ── Component ─────────────────────────────────────────────────────────────────

export function AdminNotificationBell() {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const queryClient = useQueryClient();

  // ── Queries ──────────────────────────────────────────────────────────────

  const { data: unreadCount = 0 } = useQuery({
    queryKey: ["admin-notifications-count"],
    queryFn: fetchUnreadCount,
    refetchInterval: 30_000,
    staleTime: 10_000,
  });

  // Fetch list whenever dropdown is open — keep stale cache between opens
  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ["admin-notifications"],
    queryFn: fetchNotifications,
    enabled: isOpen,
    staleTime: 30_000, // reuse cache for 30s so re-opening doesn't flicker
  });

  // ── Mutations ────────────────────────────────────────────────────────────

  const markReadMutation = useMutation({
    mutationFn: markAsReadApi,
    onMutate: async (payload) => {
      // Cancel any in-flight refetch so it doesn't overwrite optimistic state
      await queryClient.cancelQueries({ queryKey: ["admin-notifications"] });
      const prev = queryClient.getQueryData<AdminNotification[]>(["admin-notifications"]);

      // Optimistically update the list in cache RIGHT NOW
      queryClient.setQueryData<AdminNotification[]>(["admin-notifications"], (old = []) =>
        old.map((n) => (n.id === payload.resourceId ? { ...n, isRead: true } : n))
      );
      // Optimistically decrement the badge
      queryClient.setQueryData<number>(
        ["admin-notifications-count"],
        (old = 0) => Math.max(0, old - 1)
      );
      return { prev };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(["admin-notifications"], ctx.prev);
      queryClient.invalidateQueries({ queryKey: ["admin-notifications-count"] });
    },
    onSuccess: (returnedNotifId, payload) => {
      // Store the real notificationId returned by backend
      queryClient.setQueryData<AdminNotification[]>(["admin-notifications"], (old = []) =>
        old.map((n) =>
          n.id === payload.resourceId
            ? { ...n, isRead: true, notificationId: returnedNotifId }
            : n
        )
      );
    },
    onSettled: () => {
      // Sync badge with server truth
      queryClient.invalidateQueries({ queryKey: ["admin-notifications-count"] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: markAllAsReadApi,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ["admin-notifications"] });
      const prev = queryClient.getQueryData<AdminNotification[]>(["admin-notifications"]);
      queryClient.setQueryData<AdminNotification[]>(["admin-notifications"], (old = []) =>
        old.map((n) => ({ ...n, isRead: true }))
      );
      queryClient.setQueryData<number>(["admin-notifications-count"], 0);
      return { prev };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(["admin-notifications"], ctx.prev);
      queryClient.invalidateQueries({ queryKey: ["admin-notifications-count"] });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-notifications-count"] });
    },
  });

  // ── Outside-click ────────────────────────────────────────────────────────

  const handleOutsideClick = useCallback((e: MouseEvent) => {
    if (
      panelRef.current &&
      !panelRef.current.contains(e.target as Node) &&
      buttonRef.current &&
      !buttonRef.current.contains(e.target as Node)
    ) {
      setIsOpen(false);
    }
  }, []);

  useEffect(() => {
    if (isOpen) document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [isOpen, handleOutsideClick]);

  // ── Handlers ─────────────────────────────────────────────────────────────

  /**
   * Clicking a notification item:
   * - Marks it as READ in DB (await so the write completes before any refetch)
   * - Updates UI optimistically (already done in onMutate)
   * - Does NOT navigate — notification stays visible so admin sees it turned gray
   */
  const handleItemClick = async (notif: AdminNotification) => {
    if (notif.isRead) return; // already read, nothing to do
    try {
      await markReadMutation.mutateAsync({
        resourceId: notif.id,
        title: notif.title,
        description: notif.description,
        type: notif.type,
        notificationId: notif.notificationId,
      });
    } catch {
      // Error handled in onError, UI rolled back
    }
  };

  /**
   * The arrow/link button navigates to the relevant admin page.
   * If the item is unread it also marks it as read first.
   */
  const handleNavigate = async (notif: AdminNotification, e: React.MouseEvent) => {
    e.stopPropagation(); // don't trigger handleItemClick on the parent button
    const href = getHref(notif.type);

    if (!notif.isRead) {
      try {
        await markReadMutation.mutateAsync({
          resourceId: notif.id,
          title: notif.title,
          description: notif.description,
          type: notif.type,
          notificationId: notif.notificationId,
        });
      } catch {
        // continue with navigation even if mark-as-read failed
      }
    }

    setIsOpen(false);
    router.push(href);
  };

  const badge = badgeLabel(unreadCount);

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="relative">
      {/* Bell button */}
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setIsOpen((v) => !v)}
        className="relative p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors"
        aria-label={`Thông báo${badge ? ` (${badge} chưa đọc)` : ""}`}
        aria-expanded={isOpen}
        aria-haspopup="true"
      >
        <Bell className="h-5 w-5" />
        {badge !== null && (
          <span
            className="absolute top-1 right-1 min-w-[16px] h-4 flex items-center justify-center rounded-full bg-rose-500 text-[10px] font-bold text-white border-2 border-white px-0.5 leading-none"
            aria-hidden="true"
          >
            {badge}
          </span>
        )}
      </button>

      {/* Dropdown panel */}
      {isOpen && (
        <div
          ref={panelRef}
          role="dialog"
          aria-label="Thông báo quản trị"
          className="absolute right-0 top-full mt-2 w-[360px] bg-white border border-slate-200 rounded-2xl shadow-xl z-50 flex flex-col overflow-hidden"
          style={{ maxHeight: "500px" }}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 shrink-0">
            <h3 className="text-sm font-bold text-slate-900">
              Thông báo
              {unreadCount > 0 && (
                <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-700">
                  {unreadCount} chưa đọc
                </span>
              )}
            </h3>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={() => markAllMutation.mutate()}
                disabled={markAllMutation.isPending}
                className="flex items-center gap-1 text-xs text-emerald-600 hover:text-emerald-700 font-medium disabled:opacity-50 transition-colors"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                Đánh dấu đã đọc tất cả
              </button>
            )}
          </div>

          {/* List */}
          <div className="overflow-y-auto flex-1">
            {isLoading ? (
              <div className="py-10 flex flex-col items-center gap-2 text-slate-400">
                <div className="h-6 w-6 border-2 border-slate-200 border-t-emerald-500 rounded-full animate-spin" />
                <span className="text-xs">Đang tải...</span>
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-10 text-center text-slate-400 text-sm">
                <Bell className="h-8 w-8 mx-auto mb-2 opacity-30" />
                Không có thông báo nào
              </div>
            ) : (
              <ul role="list">
                {notifications.map((notif) => (
                  <li key={notif.id}>
                    {/*
                     * Row layout:
                     * [dot] [icon + title + badge]   [→ navigate button]
                     *       [description]
                     *       [timestamp]
                     *
                     * Clicking the row → mark as read (stay on page)
                     * Clicking → icon → navigate to admin page
                     */}
                    <div
                      role="button"
                      tabIndex={0}
                      onClick={() => handleItemClick(notif)}
                      onKeyDown={(e) => e.key === "Enter" && handleItemClick(notif)}
                      className={`w-full text-left px-4 py-3 flex items-start gap-3 border-b border-slate-100
                        cursor-pointer select-none transition-colors duration-150
                        ${notif.isRead
                          ? "bg-white hover:bg-slate-50"
                          : "bg-emerald-50/70 hover:bg-emerald-50"
                        }`}
                    >
                      {/* Unread dot */}
                      <span
                        className={`mt-1.5 w-2.5 h-2.5 rounded-full shrink-0 transition-all duration-300
                          ${notif.isRead
                            ? "border-2 border-slate-300 bg-transparent"
                            : "bg-emerald-500"
                          }`}
                        aria-hidden="true"
                      />

                      {/* Content */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-2 mb-0.5">
                          <div className="flex items-start gap-1.5 min-w-0">
                            <TypeIcon type={notif.type} />
                            <span className={`text-[13px] font-semibold leading-snug truncate
                              ${notif.isRead ? "text-slate-500" : "text-slate-900"}`}>
                              {notif.title}
                            </span>
                          </div>
                          {/* Read badge */}
                          {notif.isRead ? (
                            <span className="shrink-0 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-400">
                              Đã đọc
                            </span>
                          ) : (
                            <span className="shrink-0 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-100 text-emerald-700">
                              Chưa đọc
                            </span>
                          )}
                        </div>

                        <p className={`text-[12px] leading-snug line-clamp-2 mb-1
                          ${notif.isRead ? "text-slate-400" : "text-slate-600"}`}>
                          {notif.description}
                        </p>

                        <div className="flex items-center justify-between gap-2">
                          <time dateTime={notif.createdAt} className="text-[11px] text-slate-400">
                            {formatTimestamp(notif.createdAt)}
                          </time>
                          {/* Navigate button — small, on the right */}
                          <button
                            type="button"
                            onClick={(e) => handleNavigate(notif, e)}
                            className="shrink-0 flex items-center gap-1 text-[11px] text-emerald-600
                              hover:text-emerald-700 font-medium transition-colors"
                            aria-label={`Xem chi tiết ${notif.title}`}
                          >
                            <ExternalLink className="h-3 w-3" />
                            Xem
                          </button>
                        </div>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div className="px-4 py-2 border-t border-slate-100 text-center shrink-0">
              <span className="text-xs text-slate-400">{notifications.length} thông báo</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
