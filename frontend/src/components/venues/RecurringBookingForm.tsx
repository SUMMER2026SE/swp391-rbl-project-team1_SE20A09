"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { CalendarDays, Clock, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import {
  recurringBookingSchema,
  type RecurringBookingFormValues,
} from "@/lib/validations/booking.schema";
import {
  createCustomerRecurringBooking,
  type RecurringDayOfWeek,
} from "@/lib/bookings-api";
import type { TimeSlotItem, VenueDetail } from "@/lib/api/venue";

/**
 * UC-CUS-01: Form đặt sân định kỳ.
 * Tái sử dụng pattern react-hook-form + zodResolver từ register/page.tsx.
 */

const DAY_LABELS: { value: RecurringDayOfWeek; short: string; long: string }[] = [
  { value: "MONDAY", short: "T2", long: "Thứ 2" },
  { value: "TUESDAY", short: "T3", long: "Thứ 3" },
  { value: "WEDNESDAY", short: "T4", long: "Thứ 4" },
  { value: "THURSDAY", short: "T5", long: "Thứ 5" },
  { value: "FRIDAY", short: "T6", long: "Thứ 6" },
  { value: "SATURDAY", short: "T7", long: "Thứ 7" },
  { value: "SUNDAY", short: "CN", long: "Chủ nhật" },
];

/** Trả về ISO yyyy-MM-dd của hôm nay (client local time). */
function todayIso(): string {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/** Cộng N ngày vào ISO date, trả về ISO date. */
function addDays(iso: string, n: number): string {
  const d = new Date(iso);
  d.setDate(d.getDate() + n);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function formatTime(time: string): string {
  // Backend trả về "HH:mm:ss" hoặc ISO datetime; cắt lấy HH:mm.
  if (!time) return "";
  const t = time.includes("T") ? time.split("T")[1] : time;
  return t.slice(0, 5);
}

type Props = {
  venue: VenueDetail;
};

export function RecurringBookingForm({ venue }: Props) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<RecurringBookingFormValues>({
    resolver: zodResolver(recurringBookingSchema),
    defaultValues: {
      stadiumId: venue.stadiumId,
      startDate: todayIso(),
      endDate: addDays(todayIso(), 30),
      daysOfWeek: [],
      slotIds: [],
      note: "",
    },
  });

  const watchedDays = form.watch("daysOfWeek");
  const watchedStart = form.watch("startDate");
  const watchedEnd = form.watch("startDate");
  const watchedSlots = form.watch("slotIds");

  /** Ước lượng số booking sẽ được tạo — hiển thị preview cho khách trước khi submit. */
  const estimatedCount = useMemo(() => {
    if (!watchedStart || !watchedEnd || !watchedDays || !watchedSlots) return 0;
    const start = new Date(watchedStart);
    const end = new Date(watchedEnd);
    if (end < start) return 0;
    let days = 0;
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      const dow = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"][
        d.getDay()
      ] as RecurringDayOfWeek;
      if (watchedDays.includes(dow)) days++;
    }
    return days * watchedSlots.length;
  }, [watchedStart, watchedEnd, watchedDays, watchedSlots]);

  async function onSubmit(values: RecurringBookingFormValues) {
    setIsSubmitting(true);
    try {
      const result = await createCustomerRecurringBooking({
        stadiumId: values.stadiumId,
        startDate: values.startDate,
        endDate: values.endDate,
        daysOfWeek: values.daysOfWeek,
        slotIds: values.slotIds,
        note: values.note?.trim() || undefined,
      });

      toast.success(
        `Tạo thành công ${result.totalCreated} lịch đặt sân${
          result.totalSkipped > 0 ? `, ${result.totalSkipped} slot bị trùng` : ""
        }`,
        {
          description: "Đang chuyển đến trang lịch sử đặt sân...",
          action: {
            label: "Xem ngay",
            onClick: () => router.push("/bookings"),
          },
        }
      );

      // Reset form, navigate
      form.reset();
      router.push("/bookings");
    } catch (err: any) {
      const status = err?.response?.status;
      if (status === 401) {
        const redirect = `/venues/${venue.stadiumId}/recurring-booking`;
        router.push(`/login?redirect=${encodeURIComponent(redirect)}`);
        return;
      }
      const message =
        err?.response?.data?.message ??
        err?.message ??
        "Đặt sân định kỳ thất bại. Vui lòng thử lại.";
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  // Suppress unused-var warning for watchedEnd (kept for clarity / future use)
  void watchedEnd;

  // Chỉ hiển thị slot AVAILABLE để chọn (BOOKED/MAINTENANCE tự loại trừ ở backend)
  const availableSlots = venue.timeSlots.filter(
    (s: TimeSlotItem) => s.slotStatus === "AVAILABLE"
  );

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <Card className="border-emerald-200/70 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <CalendarDays className="h-5 w-5 text-emerald-700" />
              Thời gian lặp lại
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="startDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Từ ngày</FormLabel>
                    <FormControl>
                      <Input type="date" min={todayIso()} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="endDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Đến ngày</FormLabel>
                    <FormControl>
                      <Input
                        type="date"
                        min={form.watch("startDate") || todayIso()}
                        {...field}
                      />
                    </FormControl>
                    <FormDescription>Tối đa 90 ngày.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="daysOfWeek"
              render={() => (
                <FormItem>
                  <FormLabel>Các thứ trong tuần</FormLabel>
                  <FormDescription>
                    Chọn những thứ bạn muốn lặp lại trong khoảng trên.
                  </FormDescription>
                  <div className="grid grid-cols-4 sm:grid-cols-7 gap-2">
                    {DAY_LABELS.map((day) => {
                      const checked = form
                        .watch("daysOfWeek")
                        ?.includes(day.value);
                      return (
                        <label
                          key={day.value}
                          className={`flex items-center justify-center gap-2 rounded-lg border px-3 py-2 cursor-pointer text-sm font-medium transition-colors ${
                            checked
                              ? "bg-emerald-600 text-white border-emerald-600"
                              : "bg-white text-slate-700 border-slate-200 hover:border-emerald-300"
                          }`}
                        >
                          <Checkbox
                            checked={checked}
                            onCheckedChange={(c) => {
                              const current = form.getValues("daysOfWeek") ?? [];
                              const next = c
                                ? [...current, day.value]
                                : current.filter((v) => v !== day.value);
                              form.setValue("daysOfWeek", next, {
                                shouldValidate: true,
                                shouldDirty: true,
                              });
                            }}
                          />
                          <span>{day.short}</span>
                        </label>
                      );
                    })}
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card className="border-emerald-200/70 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              <Clock className="h-5 w-5 text-emerald-700" />
              Khung giờ (áp dụng cho mỗi ngày trong chuỗi)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <FormField
              control={form.control}
              name="slotIds"
              render={() => (
                <FormItem>
                  {availableSlots.length === 0 ? (
                    <p className="text-sm text-muted-foreground italic">
                      Sân này hiện chưa có khung giờ khả dụng. Vui lòng liên hệ chủ sân.
                    </p>
                  ) : (
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
                      {availableSlots.map((slot) => {
                        const checked = form
                          .watch("slotIds")
                          ?.includes(slot.slotId);
                        return (
                          <label
                            key={slot.slotId}
                            className={`flex items-center gap-2 rounded-lg border px-3 py-2 cursor-pointer text-sm font-medium transition-colors ${
                              checked
                                ? "bg-emerald-600 text-white border-emerald-600"
                                : "bg-white text-slate-700 border-slate-200 hover:border-emerald-300"
                            }`}
                          >
                            <Checkbox
                              checked={checked}
                              onCheckedChange={(c) => {
                                const current = form.getValues("slotIds") ?? [];
                                const next = c
                                  ? [...current, slot.slotId]
                                  : current.filter((v) => v !== slot.slotId);
                                form.setValue("slotIds", next, {
                                  shouldValidate: true,
                                  shouldDirty: true,
                                });
                              }}
                            />
                            <span>
                              {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
                            </span>
                          </label>
                        );
                      })}
                    </div>
                  )}
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="note"
              render={({ field }) => (
                <FormItem className="mt-4">
                  <FormLabel>Ghi chú (tuỳ chọn)</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Ví dụ: Đặt cố định cho đội bóng công ty..."
                      maxLength={500}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card className="border-emerald-200 bg-emerald-50/50 shadow-sm">
          <CardContent className="p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="text-sm text-slate-700">
                <p className="font-medium">
                  Ước tính:{" "}
                  <span className="text-emerald-700">
                    {estimatedCount} lịch đặt sân
                  </span>
                </p>
                <p className="text-xs text-muted-foreground">
                  Toàn bộ chuỗi sẽ được tạo cùng lúc (cơ chế all-or-nothing).
                </p>
              </div>
              <Button
                type="submit"
                disabled={isSubmitting || estimatedCount === 0}
                className="bg-emerald-700 hover:bg-emerald-800 text-white px-6"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Đang tạo chuỗi...
                  </>
                ) : (
                  "Xác nhận đặt định kỳ"
                )}
              </Button>
            </div>
          </CardContent>
        </Card>
      </form>
    </Form>
  );
}