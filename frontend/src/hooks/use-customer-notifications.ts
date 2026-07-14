import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { customerNotificationApi } from '@/lib/api/customer-notification-api';

const NOTIFICATION_KEYS = {
  all: ['customer-notifications'] as const,
  lists: () => [...NOTIFICATION_KEYS.all, 'list'] as const,
  list: (filters: string) => [...NOTIFICATION_KEYS.lists(), { filters }] as const,
  recent: () => [...NOTIFICATION_KEYS.all, 'recent'] as const,
  unreadCount: () => [...NOTIFICATION_KEYS.all, 'unread-count'] as const,
};

export const useCustomerNotifications = (page: number = 0, size: number = 10, unreadOnly?: boolean) => {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.list(`${page}-${size}-${unreadOnly}`),
    queryFn: () => customerNotificationApi.getNotifications(page, size, unreadOnly),
  });
};

export const useCustomerUnreadCount = () => {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.unreadCount(),
    queryFn: () => customerNotificationApi.getUnreadCount(),
    refetchInterval: 30000, // Tự động refetch mỗi 30s
  });
};

export const useCustomerRecentNotifications = (limit: number = 5) => {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.recent(),
    queryFn: () => customerNotificationApi.getRecentNotifications(limit),
  });
};

export const useMarkAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationIds: number[]) => customerNotificationApi.markAsRead(notificationIds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.all });
    },
  });
};

export const useMarkAllAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => customerNotificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.all });
    },
  });
};
