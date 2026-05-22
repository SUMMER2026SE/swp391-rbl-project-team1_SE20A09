'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Bell,
  CheckCheck,
  Calendar,
  DollarSign,
  MessageSquare,
  AlertCircle,
  Trash2,
} from "lucide-react";

export function NotificationsPage() {
  const [notifications, setNotifications] = useState([
    {
      id: 1,
      type: "booking",
      icon: <Calendar className="h-5 w-5" />,
      title: "Đặt sân thành công",
      message: "Đơn đặt sân BK001234 đã được xác nhận. Hẹn gặp bạn vào 22/05/2024 lúc 18:00.",
      time: "5 phút trước",
      read: false,
    },
    {
      id: 2,
      type: "payment",
      icon: <DollarSign className="h-5 w-5" />,
      title: "Thanh toán thành công",
      message: "Đã thanh toán 570,000đ cho đơn BK001234 qua VNPay.",
      time: "10 phút trước",
      read: false,
    },
    {
      id: 3,
      type: "message",
      icon: <MessageSquare className="h-5 w-5" />,
      title: "Tin nhắn mới",
      message: "Nguyễn Văn A đã gửi tin nhắn cho bạn.",
      time: "30 phút trước",
      read: false,
    },
    {
      id: 4,
      type: "reminder",
      icon: <Bell className="h-5 w-5" />,
      title: "Nhắc nhở đá sân",
      message: "Còn 2 giờ nữa là đến giờ đá sân tại Sân bóng Thành Công.",
      time: "1 giờ trước",
      read: true,
    },
    {
      id: 5,
      type: "system",
      icon: <AlertCircle className="h-5 w-5" />,
      title: "Chính sách hoàn tiền cập nhật",
      message: "Chúng tôi đã cập nhật chính sách hoàn tiền. Xem chi tiết tại đây.",
      time: "2 giờ trước",
      read: true,
    },
  ]);

  const markAsRead = (id: number) => {
    setNotifications(
      notifications.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const markAllAsRead = () => {
    setNotifications(notifications.map((n) => ({ ...n, read: true })));
  };

  const deleteNotification = (id: number) => {
    setNotifications(notifications.filter((n) => n.id !== id));
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  const NotificationItem = ({ notification }: { notification: typeof notifications[0] }) => (
    <Card
      className={`mb-3 ${
        notification.read ? "bg-background" : "bg-primary/5 border-primary/20"
      }`}
    >
      <CardContent className="p-4">
        <div className="flex gap-4">
          <div
            className={`${
              notification.read ? "text-muted-foreground" : "text-primary"
            } mt-1`}
          >
            {notification.icon}
          </div>

          <div className="flex-1">
            <div className="flex items-start justify-between mb-1">
              <h4 className="text-sm">{notification.title}</h4>
              <span className="text-xs text-muted-foreground">
                {notification.time}
              </span>
            </div>
            <p className="text-sm text-muted-foreground mb-3">
              {notification.message}
            </p>

            <div className="flex gap-2">
              {!notification.read && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => markAsRead(notification.id)}
                >
                  <CheckCheck className="h-4 w-4 mr-1" />
                  Đánh dấu đã đọc
                </Button>
              )}
              <Button
                size="sm"
                variant="ghost"
                onClick={() => deleteNotification(notification.id)}
              >
                <Trash2 className="h-4 w-4 mr-1" />
                Xóa
              </Button>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="max-w-3xl mx-auto">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h1 className="text-3xl mb-2">Thông báo</h1>
              {unreadCount > 0 && (
                <p className="text-muted-foreground">
                  Bạn có {unreadCount} thông báo chưa đọc
                </p>
              )}
            </div>
            {unreadCount > 0 && (
              <Button variant="outline" onClick={markAllAsRead}>
                <CheckCheck className="h-4 w-4 mr-2" />
                Đánh dấu tất cả đã đọc
              </Button>
            )}
          </div>

          <Tabs defaultValue="all">
            <TabsList className="mb-6">
              <TabsTrigger value="all">
                Tất cả
                {unreadCount > 0 && (
                  <Badge className="ml-2 bg-primary text-xs">
                    {unreadCount}
                  </Badge>
                )}
              </TabsTrigger>
              <TabsTrigger value="unread">Chưa đọc</TabsTrigger>
              <TabsTrigger value="booking">Đặt sân</TabsTrigger>
              <TabsTrigger value="message">Tin nhắn</TabsTrigger>
              <TabsTrigger value="system">Hệ thống</TabsTrigger>
            </TabsList>

            <TabsContent value="all">
              {notifications.map((notification) => (
                <NotificationItem key={notification.id} notification={notification} />
              ))}
            </TabsContent>

            <TabsContent value="unread">
              {notifications
                .filter((n) => !n.read)
                .map((notification) => (
                  <NotificationItem key={notification.id} notification={notification} />
                ))}
              {notifications.filter((n) => !n.read).length === 0 && (
                <div className="text-center py-12 text-muted-foreground">
                  <Bell className="h-12 w-12 mx-auto mb-3 opacity-50" />
                  <p>Không có thông báo chưa đọc</p>
                </div>
              )}
            </TabsContent>

            <TabsContent value="booking">
              {notifications
                .filter((n) => n.type === "booking" || n.type === "payment" || n.type === "reminder")
                .map((notification) => (
                  <NotificationItem key={notification.id} notification={notification} />
                ))}
            </TabsContent>

            <TabsContent value="message">
              {notifications
                .filter((n) => n.type === "message")
                .map((notification) => (
                  <NotificationItem key={notification.id} notification={notification} />
                ))}
            </TabsContent>

            <TabsContent value="system">
              {notifications
                .filter((n) => n.type === "system")
                .map((notification) => (
                  <NotificationItem key={notification.id} notification={notification} />
                ))}
            </TabsContent>
          </Tabs>
        </div>
      </div>

      <Footer />
    </div>
  );
}


export default NotificationsPage;
