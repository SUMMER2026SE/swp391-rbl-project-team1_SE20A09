/**
 * Notification routing logic - centralized location for determining
 * where to navigate when user clicks on different notification types
 */

import { CustomerNotificationType } from '@/types/customer-notification';

/**
 * Determine the route to navigate to based on notification type and resource ID
 * @param type - The notification type
 * @param resourceId - The related resource ID (booking ID, complaint ID, etc.)
 * @returns The route path to navigate to
 */
export function getNotificationRoute(type: CustomerNotificationType, resourceId?: string): string {
  if (!resourceId) return '#';

  switch (type) {
    case CustomerNotificationType.BOOKING_CONFIRMED:
    case CustomerNotificationType.BOOKING_CANCELLED:
    case CustomerNotificationType.PAYMENT_RECEIVED:
    case CustomerNotificationType.PAYMENT_FAILED:
    case CustomerNotificationType.REFUND_PROCESSED:
      return `/customer/bookings`;

    case CustomerNotificationType.REFUND_EXCEPTION_DECISION:
      return `/customer/refund-requests`;

    case CustomerNotificationType.COMPLAINT_ACKNOWLEDGED:
    case CustomerNotificationType.COMPLAINT_OWNER_REPLIED:
    case CustomerNotificationType.COMPLAINT_RESOLVED:
    case CustomerNotificationType.COMPLAINT_ESCALATED:
      return `/customer/complaints/${resourceId}`;

    case CustomerNotificationType.REVIEW_REMINDER:
    case CustomerNotificationType.REVIEW_OWNER_RESPONDED:
      return `/customer/reviews`;

    case CustomerNotificationType.MATCH_REQUEST_RECEIVED:
    case CustomerNotificationType.MATCH_REQUEST_APPROVED:
    case CustomerNotificationType.MATCH_REQUEST_REJECTED:
      return `/customer/matches/${resourceId}`;

    case CustomerNotificationType.UPGRADE_APPROVED:
    case CustomerNotificationType.UPGRADE_REJECTED:
      return `/customer/owner-dashboard`;

    case CustomerNotificationType.ACCOUNT_LOCKED:
    case CustomerNotificationType.ACCOUNT_UNLOCKED:
      return `/customer/settings`;

    default:
      return '#';
  }
}
