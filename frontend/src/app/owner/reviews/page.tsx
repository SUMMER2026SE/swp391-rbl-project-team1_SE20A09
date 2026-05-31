'use client'

import { useState, useEffect, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Star, MessageSquare, Send, Loader2,
  AlertCircle, RefreshCw
} from "lucide-react";
import { get, put } from "@/lib/api";

// ── Types ────────────────────────────────────────────────────
interface ReviewerInfo {
  userId: number;
  fullName: string;
  avatarUrl: string | null;
}

interface StadiumSummary {
  stadiumId: number;
  stadiumName: string;
}

interface ReviewData {
  reviewId: number;
  bookingId: number;
  reviewer: ReviewerInfo;
  stadium: StadiumSummary;
  ratingScore: number;
  comment: string;
  ownerResponse: string | null;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Component ────────────────────────────────────────────────
function ReviewManagementPage() {
  const [reviews, setReviews] = useState<ReviewData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [replyText, setReplyText] = useState("");
  const [processingId, setProcessingId] = useState<number | null>(null);

  const fetchReviews = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await get<PageResponse<ReviewData>>("/owner/reviews?size=50");
      setReviews(data.content);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Không thể tải đánh giá";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReviews();
  }, [fetchReviews]);

  const handleReply = async (reviewId: number) => {
    if (!replyText.trim()) {
      alert("Vui lòng nhập nội dung phản hồi.");
      return;
    }
    setProcessingId(reviewId);
    try {
      await put(`/owner/reviews/${reviewId}/reply`, {
        ownerResponse: replyText,
      });
      setReplyingTo(null);
      setReplyText("");
      await fetchReviews();
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Không thể gửi phản hồi";
      alert(errorMessage);
    } finally {
      setProcessingId(null);
    }
  };

  const renderStars = (score: number) => {
    return Array.from({ length: 5 }, (_, i) => (
      <Star
        key={i}
        className={`h-4 w-4 ${i < score
          ? "text-yellow-400 fill-yellow-400"
          : "text-gray-300"
        }`}
      />
    ));
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit", month: "2-digit", year: "numeric",
      hour: "2-digit", minute: "2-digit",
    });
  };

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="container mx-auto px-4 py-8">
          <Card className="p-8">
            <div className="text-center space-y-4">
              <AlertCircle className="h-12 w-12 mx-auto text-red-500" />
              <p className="text-lg text-red-600">{error}</p>
              <Button onClick={fetchReviews}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Thử lại
              </Button>
            </div>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold">Quản lý đánh giá</h1>
          <Button
            variant="outline"
            onClick={fetchReviews}
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>

        {loading ? (
          <div className="flex items-center justify-center p-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="ml-3 text-muted-foreground">Đang tải...</span>
          </div>
        ) : reviews.length === 0 ? (
          <Card className="p-8">
            <div className="text-center text-muted-foreground">
              <MessageSquare className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Chưa có đánh giá nào.</p>
            </div>
          </Card>
        ) : (
          <div className="space-y-4">
            {reviews.map((review) => (
              <Card key={review.reviewId}>
                <CardContent className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
                        {review.reviewer.fullName.charAt(0)}
                      </div>
                      <div>
                        <p className="font-semibold">{review.reviewer.fullName}</p>
                        <p className="text-sm text-muted-foreground">
                          {review.stadium.stadiumName}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="flex gap-0.5 justify-end mb-1">
                        {renderStars(review.ratingScore)}
                      </div>
                      <p className="text-xs text-muted-foreground">
                        {formatDate(review.createdAt)}
                      </p>
                    </div>
                  </div>

                  {/* Customer comment */}
                  <p className="text-sm mb-4 bg-muted/50 p-3 rounded-lg">
                    {review.comment || "Không có nhận xét."}
                  </p>

                  {/* Owner response */}
                  {review.ownerResponse ? (
                    <div className="bg-primary/5 border-l-4 border-primary p-3 rounded-r-lg">
                      <p className="text-xs font-semibold text-primary mb-1">
                        Phản hồi của bạn:
                      </p>
                      <p className="text-sm">{review.ownerResponse}</p>
                    </div>
                  ) : (
                    <div>
                      {replyingTo === review.reviewId ? (
                        <div className="space-y-2">
                          <textarea
                            placeholder="Nhập phản hồi của bạn..."
                            value={replyText}
                            onChange={(e) => setReplyText(e.target.value)}
                            className="w-full border rounded-lg px-3 py-2 text-sm min-h-[80px] resize-none"
                          />
                          <div className="flex gap-2 justify-end">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => {
                                setReplyingTo(null);
                                setReplyText("");
                              }}
                            >
                              Hủy
                            </Button>
                            <Button
                              size="sm"
                              onClick={() => handleReply(review.reviewId)}
                              disabled={processingId === review.reviewId}
                            >
                              {processingId === review.reviewId ? (
                                <Loader2 className="h-4 w-4 animate-spin mr-1" />
                              ) : (
                                <Send className="h-4 w-4 mr-1" />
                              )}
                              Gửi phản hồi
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setReplyingTo(review.reviewId)}
                        >
                          <MessageSquare className="h-4 w-4 mr-1" />
                          Phản hồi
                        </Button>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default ReviewManagementPage;
