export type { PageResponse, ApiResponse } from './common';

export enum NotificationType {
  BOOKING = 'BOOKING',
  PAYMENT = 'PAYMENT',
  PROMOTION = 'PROMOTION',
  SYSTEM = 'SYSTEM',
  REVIEW = 'REVIEW',
  COMPLAINT = 'COMPLAINT',
  OWNER_APPROVAL = 'OWNER_APPROVAL',
  STADIUM_APPROVAL = 'STADIUM_APPROVAL',
  ACCOUNT_LOCK = 'ACCOUNT_LOCK',
  APPEAL = 'APPEAL',
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
