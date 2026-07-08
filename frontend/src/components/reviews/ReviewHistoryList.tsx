"use client";

import { useState, useEffect } from "react";
import { get } from "@/lib/api";
import { Star, Loader2, MessageSquare, ChevronLeft, ChevronRight } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface ReviewResponse {
  reviewId: number;
  bookingId: number;
  stadiumId: number;
  reviewerName: string;
  ratingScore: number;
  comment: string;
  ownerResponse: string;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export function ReviewHistoryList() {
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const fetchReviews = async () => {
      try {
        setLoading(true);
        const data = await get<PageResponse<ReviewResponse>>(
          `/reviews/me?page=${page}&size=10&sort=createdAt,desc`
        );
        setReviews(data.content);
        setTotalPages(data.totalPages);
        setError(null);
      } catch (err: any) {
        setError(err.message || "Không thể tải danh sách đánh giá");
      } finally {
        setLoading(false);
      }
    };
    fetchReviews();
  }, [page]);

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-8 text-red-500">
        <p>{error}</p>
      </div>
    );
  }

  if (reviews.length === 0) {
    return (
      <Card className="border-none shadow-sm bg-white p-8 text-center">
        <MessageSquare className="h-16 w-16 text-slate-300 mx-auto mb-4" />
        <h3 className="text-lg font-bold text-slate-800 mb-2">Chưa có đánh giá nào</h3>
        <p className="text-slate-500 text-sm">Bạn chưa viết đánh giá nào cho các sân đã đặt.</p>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {reviews.map((review) => (
        <Card key={review.reviewId} className="border-slate-100 shadow-sm hover:shadow-md transition-shadow">
          <CardContent className="p-5">
            <div className="flex justify-between items-start mb-3">
              <div>
                <h4 className="font-semibold text-slate-800 flex items-center gap-2">
                  Mã đặt sân: #{review.bookingId}
                  <Badge variant="outline" className="text-xs font-normal">
                    {new Date(review.createdAt).toLocaleDateString("vi-VN")}
                  </Badge>
                </h4>
              </div>
              <div className="flex text-amber-400">
                {[1, 2, 3, 4, 5].map((star) => (
                  <Star
                    key={star}
                    className={`h-4 w-4 ${star <= review.ratingScore ? "fill-current" : "text-slate-200"}`}
                  />
                ))}
              </div>
            </div>
            {review.comment && (
              <p className="text-slate-600 text-sm italic border-l-2 border-slate-200 pl-3">
                &quot;{review.comment}&quot;
              </p>
            )}
            {review.ownerResponse && (
              <div className="mt-3 bg-slate-50 rounded-lg p-3 text-sm">
                <p className="font-semibold text-slate-700 text-xs mb-1">Phản hồi từ chủ sân:</p>
                <p className="text-slate-600">{review.ownerResponse}</p>
              </div>
            )}
          </CardContent>
        </Card>
      ))}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-4">
          <Button
            type="button"
            variant="outline"
            size="icon"
            onClick={() => setPage(current => Math.max(0, current - 1))}
            disabled={page === 0 || loading}
            aria-label="Trang đánh giá trước"
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm font-medium text-slate-600">
            Trang {page + 1} / {totalPages}
          </span>
          <Button
            type="button"
            variant="outline"
            size="icon"
            onClick={() => setPage(current => Math.min(totalPages - 1, current + 1))}
            disabled={page >= totalPages - 1 || loading}
            aria-label="Trang đánh giá sau"
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
