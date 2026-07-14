"use client";

import { useState } from "react";
import { Bell } from "lucide-react";
import { useRouter } from "next/navigation";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import {
  useCustomerRecentNotifications,
  useCustomerUnreadCount,
  useMarkAsRead,
  useMarkAllAsRead,
} from "@/hooks/use-customer-notifications";
import { CustomerNotificationItem } from "./CustomerNotificationItem";
import { NotificationType } from "@/types/notification";

function getNotificationRoute(type: NotificationType, resourceId?: string): string {
  switch (type) {
    case NotificationType.BOOKING_CONFIRMED:
    case NotificationType.BOOKING_CANCELLED:
    case NotificationType.PAYMENT_RECEIVED:
    case NotificationType.PAYMENT_FAILED:
    case NotificationType.REFUND_PROCESSED:
    case NotificationType.REFUND_EXCEPTION_DECISION:
      return resourceId ? `/booking/${resourceId}` : "/bookings";

    case NotificationType.COMPLAINT_ACKNOWLEDGED:
    case NotificationType.COMPLAINT_OWNER_REPLIED:
    case NotificationType.COMPLAINT_RESOLVED:
    case NotificationType.COMPLAINT_ESCALATED:
      return resourceId ? `/complaints/${resourceId}` : "/complaints";

    case NotificationType.REVIEW_REMINDER:
    case NotificationType.REVIEW_OWNER_RESPONDED:
      // Show booking that needs review
      return "/bookings";

    case NotificationType.MATCH_REQUEST_RECEIVED:
    case NotificationType.MATCH_REQUEST_APPROVED:
    case NotificationType.MATCH_REQUEST_REJECTED:
    case NotificationType.MATCH_CANCELLED:
      // resourceId is matchId — navigate to community page with matchId query param
      return resourceId ? `/community?matchId=${resourceId}` : "/community";

    case NotificationType.UPGRADE_APPROVED:
      return "/owner";
    case NotificationType.UPGRADE_REJECTED:
      return "/profile";

    case NotificationType.ACCOUNT_LOCKED:
    case NotificationType.ACCOUNT_UNLOCKED:
      return "/profile";

    default:
      return "/notifications";
  }
}

export function CustomerNotificationBell() {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);

  const { data: unreadCount = 0 } = useCustomerUnreadCount();
  const { data: recentNotifications = [] } = useCustomerRecentNotifications(5);
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();

  const handleNotificationClick = (notificationId: number, resourceId?: string) => {
    const notification = recentNotifications.find((n) => n.notificationId === notificationId);
    markAsRead.mutate([notificationId]);
    setIsOpen(false);
    const route = notification
      ? getNotificationRoute(notification.notificationType, resourceId)
      : "/notifications";
    router.push(route);
  };

  const handleMarkAllAsRead = () => {
    markAllAsRead.mutate();
  };

  const handleViewAll = () => {
    setIsOpen(false);
    router.push("/notifications");
  };

  return (
    <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <Badge
              variant="destructive"
              className="absolute -top-1 -right-1 h-5 w-5 flex items-center justify-center p-0 text-[10px]"
            >
              {unreadCount > 99 ? "99+" : unreadCount}
            </Badge>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-[380px] p-0">
        <div className="flex flex-row items-center justify-between p-4 border-b">
          <div className="font-semibold text-lg">Thông báo</div>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-auto p-0 text-primary hover:text-primary/80 hover:bg-transparent"
              onClick={handleMarkAllAsRead}
              disabled={markAllAsRead.isPending}
            >
              Đánh dấu tất cả đã đọc
            </Button>
          )}
        </div>

        <ScrollArea className="h-[400px]">
          {recentNotifications.length > 0 ? (
            <div className="flex flex-col">
              {recentNotifications.map((notification) => (
                <CustomerNotificationItem
                  key={notification.notificationId}
                  notification={notification}
                  onClick={handleNotificationClick}
                />
              ))}
            </div>
          ) : (
            <div className="p-8 text-center text-muted-foreground flex flex-col items-center justify-center h-full space-y-3">
              <Bell className="h-10 w-10 text-muted-foreground/30" />
              <p>Bạn không có thông báo nào mới</p>
            </div>
          )}
        </ScrollArea>

        <div className="p-2 border-t">
          <Button
            variant="ghost"
            className="w-full justify-center text-primary"
            onClick={handleViewAll}
          >
            Xem tất cả thông báo
          </Button>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
