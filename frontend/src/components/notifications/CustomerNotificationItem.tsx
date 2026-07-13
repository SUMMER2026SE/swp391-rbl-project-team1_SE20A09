"use client";

import {
  Bell,
  Calendar,
  CreditCard,
  Star,
  MessageSquare,
  AlertTriangle,
  Info,
  CheckCircle,
  XCircle,
  Clock,
  ShieldAlert,
} from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { vi } from "date-fns/locale";
import { NotificationResponse, NotificationType } from "@/types/notification";

interface CustomerNotificationItemProps {
  notification: NotificationResponse;
  onClick: (id: number, resourceId?: string) => void;
}

export function CustomerNotificationItem({
  notification,
  onClick,
}: CustomerNotificationItemProps) {
  const getIcon = (type: NotificationType) => {
    switch (type) {
      case NotificationType.BOOKING_CONFIRMED:
        return <Calendar className="h-5 w-5 text-green-500" />;
      case NotificationType.BOOKING_CANCELLED:
        return <XCircle className="h-5 w-5 text-red-500" />;
      case NotificationType.PAYMENT_RECEIVED:
        return <CreditCard className="h-5 w-5 text-blue-500" />;
      case NotificationType.PAYMENT_FAILED:
        return <AlertTriangle className="h-5 w-5 text-red-500" />;
      case NotificationType.REFUND_PROCESSED:
      case NotificationType.REFUND_EXCEPTION_DECISION:
        return <Clock className="h-5 w-5 text-orange-500" />;
      case NotificationType.COMPLAINT_ACKNOWLEDGED:
      case NotificationType.COMPLAINT_OWNER_REPLIED:
      case NotificationType.COMPLAINT_RESOLVED:
      case NotificationType.COMPLAINT_ESCALATED:
        return <MessageSquare className="h-5 w-5 text-purple-500" />;
      case NotificationType.REVIEW_REMINDER:
      case NotificationType.REVIEW_OWNER_RESPONDED:
        return <Star className="h-5 w-5 text-yellow-500" />;
      case NotificationType.MATCH_REQUEST_RECEIVED:
      case NotificationType.MATCH_REQUEST_APPROVED:
      case NotificationType.MATCH_REQUEST_REJECTED:
      case NotificationType.MATCH_CANCELLED:
        return <CheckCircle className="h-5 w-5 text-indigo-500" />;
      case NotificationType.UPGRADE_APPROVED:
      case NotificationType.UPGRADE_REJECTED:
        return <ShieldAlert className="h-5 w-5 text-amber-500" />;
      case NotificationType.ACCOUNT_LOCKED:
      case NotificationType.ACCOUNT_UNLOCKED:
        return <AlertTriangle className="h-5 w-5 text-red-500" />;
      default:
        return <Bell className="h-5 w-5 text-gray-500" />;
    }
  };

  return (
    <div
      onClick={() => onClick(notification.notificationId, notification.relatedResourceId)}
      className={`p-4 border-b last:border-b-0 cursor-pointer transition-colors hover:bg-muted/50 ${
        !notification.isRead ? "bg-primary/5" : ""
      }`}
    >
      <div className="flex gap-3">
        <div className="flex-shrink-0 mt-1">{getIcon(notification.notificationType)}</div>
        <div className="flex-1 space-y-1">
          <p className={`text-sm ${!notification.isRead ? "font-semibold" : "font-medium"}`}>
            {notification.title}
          </p>
          <p className="text-sm text-muted-foreground line-clamp-2">
            {notification.message}
          </p>
          <p className="text-xs text-muted-foreground">
            {formatDistanceToNow(new Date(notification.createdAt), {
              addSuffix: true,
              locale: vi,
            })}
          </p>
        </div>
        {!notification.isRead && (
          <div className="flex-shrink-0 flex items-center">
            <span className="h-2 w-2 bg-primary rounded-full" />
          </div>
        )}
      </div>
    </div>
  );
}
