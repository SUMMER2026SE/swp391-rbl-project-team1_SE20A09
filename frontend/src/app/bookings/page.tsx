import { redirect } from 'next/navigation';

export default function BookingHistoryRedirect() {
  redirect('/profile?tab=bookings');
}
