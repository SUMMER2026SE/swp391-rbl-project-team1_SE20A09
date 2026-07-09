import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

/**
 * shadcn/ui utility: merge Tailwind classes safely
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Tính toán phí dịch vụ nền tảng động dựa trên giá sân: 5% giá sân, sàn 10k, trần 30k.
 */
export function calculatePlatformFee(courtPrice: number): number {
  const rawFee = Math.round(courtPrice * 0.05);
  if (rawFee < 10000) return 10000;
  if (rawFee > 30000) return 30000;
  return rawFee;
}
