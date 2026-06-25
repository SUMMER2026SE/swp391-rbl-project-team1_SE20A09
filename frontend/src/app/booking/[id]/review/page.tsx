"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Star, Send } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { post } from "@/lib/api";

export default function ReviewPage() {
  const router = useRouter();
  const params = useParams();
  const id = params?.id as string;
  
  const [rating, setRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [comment, setComment] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (rating === 0 || !id) return;
    
    setIsSubmitting(true);
    setError(null);
    try {
      await post(`/reviews/bookings/${id}/reviews`, {
        ratingScore: rating,
        comment: comment
      });
      setIsSuccess(true);
      setTimeout(() => {
        router.push('/profile');
      }, 2000);
    } catch (err: any) {
      setError(err.message || "Không thể gửi đánh giá. Có thể bạn đã đánh giá rồi hoặc lỗi hệ thống.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Header />
      <div className="flex-1 container mx-auto px-4 py-8 max-w-2xl">
        <Link href={`/booking/${id}`} className="inline-flex items-center text-sm text-slate-500 hover:text-slate-900 mb-6 transition-colors">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Quay lại chi tiết đặt sân
        </Link>

        <Card className="border-none shadow-md bg-white">
          <CardHeader className="text-center pb-2">
            <div className="mx-auto w-12 h-12 bg-amber-50 rounded-full flex items-center justify-center mb-4">
              <Star className="h-6 w-6 text-amber-500" />
            </div>
            <CardTitle className="text-2xl font-bold">Đánh giá trải nghiệm</CardTitle>
            <CardDescription className="text-base">
              Mã đặt sân: #{id}
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-6">
            {isSuccess ? (
              <div className="text-center py-8">
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 mb-4">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
                </div>
                <h3 className="text-xl font-bold text-slate-800 mb-2">Gửi đánh giá thành công!</h3>
                <p className="text-slate-500">Cảm ơn bạn đã chia sẻ trải nghiệm. Đang chuyển hướng...</p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-6">
                <div className="flex flex-col items-center space-y-3">
                  <p className="text-sm font-medium text-slate-700">Bạn cảm thấy sân bãi và dịch vụ thế nào?</p>
                  <div className="flex gap-2">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setRating(star)}
                        onMouseEnter={() => setHoveredRating(star)}
                        onMouseLeave={() => setHoveredRating(0)}
                        className="p-1 transition-transform hover:scale-110 focus:outline-none"
                      >
                        <Star
                          className={`h-10 w-10 ${
                            star <= (hoveredRating || rating)
                              ? "fill-amber-400 text-amber-400"
                              : "text-slate-200"
                          } transition-colors`}
                        />
                      </button>
                    ))}
                  </div>
                  {rating > 0 && (
                    <span className="text-sm font-semibold text-amber-600 bg-amber-50 px-3 py-1 rounded-full">
                      {["Rất tệ", "Tệ", "Bình thường", "Tốt", "Tuyệt vời"][rating - 1]}
                    </span>
                  )}
                </div>

                <div className="space-y-3">
                  <label htmlFor="comment" className="text-sm font-medium text-slate-700">
                    Chia sẻ thêm về trải nghiệm của bạn <span className="text-red-500">*</span>
                  </label>
                  <Textarea
                    id="comment"
                    placeholder="Sân sạch sẽ, chủ sân nhiệt tình..."
                    rows={4}
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    className="resize-none bg-slate-50 border-slate-200 focus-visible:ring-amber-500"
                  />
                </div>

                {error && (
                  <div className="p-3 bg-red-50 text-red-600 text-sm rounded-lg border border-red-100">
                    {error}
                  </div>
                )}

                <Button 
                  type="submit" 
                  disabled={rating === 0 || comment.trim() === "" || isSubmitting}
                  className="w-full h-12 text-base font-semibold bg-primary hover:bg-primary/90"
                >
                  {isSubmitting ? "Đang gửi..." : (
                    <>
                      <Send className="mr-2 h-4 w-4" /> Gửi đánh giá
                    </>
                  )}
                </Button>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
      <Footer />
    </div>
  );
}
