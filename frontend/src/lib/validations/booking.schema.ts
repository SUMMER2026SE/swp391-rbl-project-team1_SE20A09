import { z } from "zod";

/**
 * UC-CUS-01: Zod schema cho form đặt sân định kỳ.
 * Phía client validate trước khi gửi API — backend cũng validate (CreateCustomerRecurringBookingRequest.isValidDateRange).
 * Giữ message tiếng Việt để UX nhất quán với các form khác.
 */

const DAY_OF_WEEK = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
] as const;

const today = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
};

const toIsoDate = (d: Date) => {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

const MIN_DATE = toIsoDate(today());

export const recurringBookingSchema = z
  .object({
    stadiumId: z.number().int().positive("Vui lòng chọn sân"),
    startDate: z
      .string()
      .min(1, "Vui lòng chọn ngày bắt đầu")
      .refine((v) => v >= MIN_DATE, {
        message: "Ngày bắt đầu không được trước hôm nay",
      }),
    endDate: z
      .string()
      .min(1, "Vui lòng chọn ngày kết thúc"),
    daysOfWeek: z
      .array(z.enum(DAY_OF_WEEK))
      .min(1, "Vui lòng chọn ít nhất một thứ trong tuần")
      .max(7, "Không được chọn quá 7 thứ"),
    slotIds: z
      .array(z.number().int().positive())
      .min(1, "Vui lòng chọn ít nhất một khung giờ"),
    note: z
      .string()
      .max(500, "Ghi chú không được vượt quá 500 ký tự")
      .optional()
      .or(z.literal("")),
  })
  .refine((data) => data.startDate <= data.endDate, {
    message: "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu",
    path: ["endDate"],
  })
  .refine(
    (data) => {
      const start = new Date(data.startDate);
      const end = new Date(data.endDate);
      const diffDays = Math.floor(
        (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)
      );
      return diffDays <= 90;
    },
    {
      message: "Chuỗi đặt sân không được vượt quá 90 ngày",
      path: ["endDate"],
    }
  );

export type RecurringBookingFormValues = z.infer<typeof recurringBookingSchema>;