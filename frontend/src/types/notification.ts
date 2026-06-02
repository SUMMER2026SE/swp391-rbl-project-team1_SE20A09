export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

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
