import api from '../api';
import { ApiResponse, PageResponse } from '@/types/common';
import { NotificationResponse } from '@/types/notification';

export const customerNotificationApi = {
  getNotifications: async (
    page: number = 0,
    size: number = 10,
    unreadOnly?: boolean
  ): Promise<PageResponse<NotificationResponse>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (unreadOnly !== undefined) {
      params.append('unreadOnly', unreadOnly.toString());
    }
    const response = await api.get<ApiResponse<PageResponse<NotificationResponse>>>(`/customer/notifications?${params.toString()}`);
    return response.data.result!;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await api.get<ApiResponse<number>>('/customer/notifications/unread-count');
    return response.data.result!;
  },

  getRecentNotifications: async (limit: number = 5): Promise<NotificationResponse[]> => {
    const response = await api.get<ApiResponse<NotificationResponse[]>>(`/customer/notifications/recent?limit=${limit}`);
    return response.data.result!;
  },

  markAsRead: async (notificationIds: number[]): Promise<void> => {
    await api.patch<ApiResponse<void>>('/customer/notifications/mark-as-read', notificationIds);
  },

  markAllAsRead: async (): Promise<void> => {
    await api.patch<ApiResponse<void>>('/customer/notifications/mark-all-as-read');
  },
};
