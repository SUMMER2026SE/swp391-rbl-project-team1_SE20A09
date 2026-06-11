'use client'

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { AlertCircle, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { put } from "@/lib/api";

export default function CancelBookingPage() {
  const params = useParams();
  const router = useRouter();
  const bookingId = params?.id as string;

  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleCancel = async () => {
    if (!reason.trim()) {
      toast.error("Vui lòng nhập lý do huỷ đặt sân");
      return;
    }

    try {
      setSubmitting(true);
      await put(`/bookings/${bookingId}/cancel`, { reason });
      toast.success("Huỷ đặt sân thành công");
      router.refresh();
      router.push("/bookings");
    } catch (err: any) {
      toast.error(err.message || "Có lỗi xảy ra khi huỷ đặt sân");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-muted/30 p-4 sm:p-8 flex justify-center items-center">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center gap-2 mb-2">
            <Link href={`/booking/${bookingId}`}>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <CardTitle>Xác nhận huỷ đặt sân</CardTitle>
          </div>
          <CardDescription>
            Bạn đang yêu cầu huỷ đơn đặt sân #{bookingId}. Vui lòng cho chúng tôi biết lý do.
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="rounded-md bg-yellow-50 p-4 border border-yellow-200">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertCircle className="h-5 w-5 text-yellow-600" aria-hidden="true" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-yellow-800">Lưu ý về chính sách hoàn tiền</h3>
                <div className="mt-2 text-sm text-yellow-700">
                  <p>
                    - Huỷ trước 24h: Hoàn 100% tiền cọc.
                    <br />
                    - Huỷ trước 12h: Hoàn 50% tiền cọc.
                    <br />
                    - Huỷ dưới 12h: Không hoàn tiền cọc.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="reason">Lý do huỷ (Bắt buộc) *</Label>
            <Textarea
              id="reason"
              placeholder="VD: Thay đổi lịch trình trình, thời tiết xấu..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required
              rows={4}
            />
          </div>
        </CardContent>

        <CardFooter className="flex gap-2">
          <Link href={`/booking/${bookingId}`} className="flex-1">
            <Button variant="outline" className="w-full">
              Trở lại
            </Button>
          </Link>
          <Button 
            variant="destructive" 
            className="flex-1"
            onClick={handleCancel}
            disabled={submitting || !reason.trim()}
          >
            {submitting ? "Đang xử lý..." : "Xác nhận huỷ"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
