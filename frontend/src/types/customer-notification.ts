/**
 * Customer-specific notification types
 * These are the notification types that can be sent to customers
 */

export enum CustomerNotificationType {
  // Booking related
  BOOKING_CONFIRMED = 'BOOKING_CONFIRMED',
  BOOKING_CANCELLED = 'BOOKING_CANCELLED',

  // Payment related
  PAYMENT_RECEIVED = 'PAYMENT_RECEIVED',
  PAYMENT_FAILED = 'PAYMENT_FAILED',
  REFUND_PROCESSED = 'REFUND_PROCESSED',
  REFUND_EXCEPTION_DECISION = 'REFUND_EXCEPTION_DECISION',

  // Complaint related
  COMPLAINT_ACKNOWLEDGED = 'COMPLAINT_ACKNOWLEDGED',
  COMPLAINT_OWNER_REPLIED = 'COMPLAINT_OWNER_REPLIED',
  COMPLAINT_RESOLVED = 'COMPLAINT_RESOLVED',
  COMPLAINT_ESCALATED = 'COMPLAINT_ESCALATED',

  // Review related
  REVIEW_REMINDER = 'REVIEW_REMINDER',
  REVIEW_OWNER_RESPONDED = 'REVIEW_OWNER_RESPONDED',

  // Match related
  MATCH_REQUEST_RECEIVED = 'MATCH_REQUEST_RECEIVED',
  MATCH_REQUEST_APPROVED = 'MATCH_REQUEST_APPROVED',
  MATCH_REQUEST_REJECTED = 'MATCH_REQUEST_REJECTED',

  // Account/Upgrade related
  UPGRADE_APPROVED = 'UPGRADE_APPROVED',
  UPGRADE_REJECTED = 'UPGRADE_REJECTED',
  ACCOUNT_LOCKED = 'ACCOUNT_LOCKED',
  ACCOUNT_UNLOCKED = 'ACCOUNT_UNLOCKED',
}

export interface CustomerNotification {
  notificationId: number;
  type: CustomerNotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  relatedResourceId?: string;
}
