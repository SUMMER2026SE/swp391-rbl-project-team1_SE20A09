"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Bell, CheckCheck, Loader2 } from "lucide-react";
import {
  useCustomerNotifications,
  useCustomerUnreadCount,
  useMarkAsRead,
  useMarkAllAsRead,
} from "@/hooks/use-customer-notifications";
import { CustomerNotificationItem } from "@/components/notifications/CustomerNotificationItem";
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
      return "/bookings";

    case NotificationType.MATCH_REQUEST_RECEIVED:
    case NotificationType.MATCH_REQUEST_APPROVED:
    case NotificationType.MATCH_REQUEST_REJECTED:
    case NotificationType.MATCH_CANCELLED:
      return resourceId ? `/community?matchId=${resourceId}` : "/community";

    case NotificationType.UPGRADE_APPROVED:
      return "/owner";
    case NotificationType.UPGRADE_REJECTED:
      return "/profile";

    case NotificationType.ACCOUNT_LOCKED:
    case NotificationType.ACCOUNT_UNLOCKED:
      return "/profile";

    default:
      return "";
  }
}

function NotificationsPage() {
  const router = useRouter();
  const [tab, setTab] = useState<"all" | "unread">("all");
  const [page, setPage] = useState(0);

  const { data: notificationsData, isLoading } = useCustomerNotifications(
    page,
    20,
    tab === "unread" ? true : undefined
  );
  const { data: unreadCount = 0 } = useCustomerUnreadCount();

  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();

  const handleMarkAllAsRead = () => {
    markAllAsRead.mutate();
  };

  const handleNotificationClick = (id: number, resourceId?: string) => {
    const allNotifications = notificationsData?.content ?? [];
    const notification = allNotifications.find((n) => n.notificationId === id);
    markAsRead.mutate([id]);
    if (notification) {
      const route = getNotificationRoute(notification.notificationType, resourceId);
      if (route) router.push(route);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />

      <div className="flex-1 container mx-auto px-4 py-8">
        <div className="max-w-3xl mx-auto">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h1 className="text-3xl font-bold mb-2">Thông báo</h1>
              {unreadCount > 0 && (
                <p className="text-muted-foreground">
                  Bạn có {unreadCount} thông báo chưa đọc
                </p>
              )}
            </div>
            {unreadCount > 0 && (
              <Button variant="outline" onClick={handleMarkAllAsRead} disabled={markAllAsRead.isPending}>
                <CheckCheck className="h-4 w-4 mr-2" />
                Đánh dấu tất cả đã đọc
              </Button>
            )}
          </div>

          <Card className="shadow-sm">
            <CardContent className="p-0">
              <Tabs
                defaultValue="all"
                value={tab}
                onValueChange={(val) => {
                  setTab(val as "all" | "unread");
                  setPage(0);
                }}
              >
                <div className="border-b px-4 py-2">
                  <TabsList className="bg-transparent">
                    <TabsTrigger
                      value="all"
                      className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary rounded-none px-4"
                    >
                      Tất cả
                      {unreadCount > 0 && (
                        <Badge className="ml-2 bg-primary/10 text-primary hover:bg-primary/20">
                          {unreadCount}
                        </Badge>
                      )}
                    </TabsTrigger>
                    <TabsTrigger
                      value="unread"
                      className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary rounded-none px-4"
                    >
                      Chưa đọc
                    </TabsTrigger>
                  </TabsList>
                </div>

                <TabsContent value="all" className="m-0 border-none p-0 outline-none">
                  {isLoading ? (
                    <div className="flex justify-center p-12">
                      <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                    </div>
                  ) : notificationsData?.content.length ? (
                    <div className="flex flex-col">
                      {notificationsData.content.map((notification) => (
                        <CustomerNotificationItem
                          key={notification.notificationId}
                          notification={notification}
                          onClick={handleNotificationClick}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-16 text-muted-foreground">
                      <Bell className="h-12 w-12 mx-auto mb-4 text-muted-foreground/30" />
                      <p>Không có thông báo nào</p>
                    </div>
                  )}
                </TabsContent>

                <TabsContent value="unread" className="m-0 border-none p-0 outline-none">
                  {isLoading ? (
                    <div className="flex justify-center p-12">
                      <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                    </div>
                  ) : notificationsData?.content.length ? (
                    <div className="flex flex-col">
                      {notificationsData.content.map((notification) => (
                        <CustomerNotificationItem
                          key={notification.notificationId}
                          notification={notification}
                          onClick={handleNotificationClick}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-16 text-muted-foreground">
                      <CheckCheck className="h-12 w-12 mx-auto mb-4 text-muted-foreground/30" />
                      <p>Bạn đã đọc tất cả thông báo</p>
                    </div>
                  )}
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>

          {/* Pagination */}
          {notificationsData && notificationsData.totalPages > 1 && (
            <div className="flex justify-center items-center mt-6 gap-3">
              <Button
                variant="outline"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Trang trước
              </Button>
              <span className="text-sm text-muted-foreground">
                Trang {page + 1} / {notificationsData.totalPages}
              </span>
              <Button
                variant="outline"
                disabled={page >= notificationsData.totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Trang sau
              </Button>
            </div>
          )}
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default NotificationsPage;
