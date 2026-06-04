export type { PageResponse, ApiResponse } from './common';

export enum NotificationType {
  BOOKING = 'BOOKING',
  PAYMENT = 'PAYMENT',
  PROMOTION = 'PROMOTION',
  SYSTEM = 'SYSTEM',
  REVIEW = 'REVIEW',
  COMPLAINT = 'COMPLAINT',
}

export interface NotificationResponse {
  notificationId: number;
  notificationType: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  relatedResourceId?: string;
}
