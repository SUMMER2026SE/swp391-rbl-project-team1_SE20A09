'use client'

import { useState, useEffect } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Star,
  MessageSquare,
  Home,
  BarChart3,
  Calendar,
  AlertCircle,
  Clock,
  User,
  Send
} from "lucide-react";
import { useRouter } from "next/navigation";
import { get, post } from "@/lib/api";
import { toast } from "sonner";

function OwnerReviewsPage() {
  const router = useRouter();
  const [reviews, setReviews] = useState<any[]>([]);
  const [replyText, setReplyText] = useState<{ [key: string]: string }>({});
  const [filterRating, setFilterRating] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");

  const DEFAULT_REVIEWS = [
    {
      id: "REV001",
      stadiumName: "Sân bóng Thành Công - Sân 1",
      customerName: "Nguyễn Văn A",
      rating: 5,
      comment: "Sân rất đẹp, cỏ nhân tạo mượt mà, ánh sáng tốt vào buổi tối. Nhân viên phục vụ nhiệt tình.",
      tags: ["Cỏ đẹp", "Ánh sáng tốt", "Giá hợp lý"],
      reviewDate: "2026-05-24",
      bookingId: "BK001236",
      ownerResponse: "",
    },
    {
      id: "REV002",
      stadiumName: "Sân bóng Thành Công - Sân 2",
      customerName: "Trần Thị B",
      rating: 4,
      comment: "Sân tương đối tốt, tuy nhiên hơi ít chỗ đỗ xe vào giờ cao điểm.",
      tags: ["Chất lượng tốt", "Hơi thiếu chỗ đỗ xe"],
      reviewDate: "2026-05-20",
      bookingId: "BK001235",
      ownerResponse: "Cảm ơn bạn đã phản hồi. Chúng tôi đang lên kế hoạch mở rộng khu vực đỗ xe để phục vụ khách tốt hơn.",
    },
  ];

  const fetchReviews = async () => {
    try {
      const data = await get<any[]>("/owner/reviews");
      const mapped = data.map((r: any) => ({
        id: r.id,
        reviewId: r.reviewId,
        stadiumName: r.stadiumName || r.venueName,
        bookingId: r.bookingId,
        customerName: r.customerName,
        reviewDate: r.createdAt || r.reviewDate,
        rating: r.rating,
        comment: r.comment,
        tags: r.tags || [],
        ownerResponse: r.ownerResponse
      }));
      setReviews(mapped);
    } catch (err: any) {
      toast.error(err.message || "Không thể tải danh sách đánh giá.");
    }
  };

  useEffect(() => {
    fetchReviews();
  }, []);

  const handleSendReply = async (reviewId: string) => {
    const text = replyText[reviewId];
    if (!text || !text.trim()) return;

    try {
      const numericId = typeof reviewId === 'string' && reviewId.startsWith("REV-")
        ? parseInt(reviewId.replace("REV-", ""), 10)
        : parseInt(reviewId, 10);

      await post<any>(`/owner/reviews/${numericId}/reply`, { ownerResponse: text.trim() });
      
      setReviews(prev => prev.map(r => {
        const rId = typeof r.id === 'string' && r.id.startsWith("REV-") 
          ? parseInt(r.id.replace("REV-", ""), 10) 
          : parseInt(r.id, 10);
        return rId === numericId ? { ...r, ownerResponse: text.trim() } : r;
      }));
      setReplyText(prev => ({ ...prev, [reviewId]: "" }));
      toast.success("Gửi phản hồi thành công!");
    } catch (err: any) {
      toast.error(err.message || "Không thể gửi phản hồi.");
    }
  };

  const handleDeleteReply = async (reviewId: string) => {
    try {
      const numericId = typeof reviewId === 'string' && reviewId.startsWith("REV-")
        ? parseInt(reviewId.replace("REV-", ""), 10)
        : parseInt(reviewId, 10);

      await post<any>(`/owner/reviews/${numericId}/reply`, { ownerResponse: "" });
      
      setReviews(prev => prev.map(r => {
        const rId = typeof r.id === 'string' && r.id.startsWith("REV-") 
          ? parseInt(r.id.replace("REV-", ""), 10) 
          : parseInt(r.id, 10);
        return rId === numericId ? { ...r, ownerResponse: "" } : r;
      }));
      toast.success("Xóa phản hồi thành công!");
    } catch (err: any) {
      toast.error(err.message || "Không thể xóa phản hồi.");
    }
  };

  const getFilteredReviews = () => {
    return reviews.filter(r => {
      const matchesRating = filterRating === "all" || r.rating.toString() === filterRating;
      const matchesSearch = searchTerm === "" ||
        r.stadiumName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (r.customerName || "Khách ẩn danh").toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.comment.toLowerCase().includes(searchTerm.toLowerCase());
      return matchesRating && matchesSearch;
    });
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="flex">
        {/* Sidebar */}
        <aside className="w-64 min-h-[calc(100vh-64px)] bg-card border-r p-4 shrink-0">
          <h2 className="text-xl font-bold mb-6 px-3 text-primary">Quản lý chủ sân</h2>
          <nav className="space-y-1">
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/dashboard")}
            >
              <Home className="mr-3 h-4 w-4" />
              Dashboard
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/venues")}
            >
              <BarChart3 className="mr-3 h-4 w-4" />
              Sân của tôi
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/bookings")}
            >
              <Calendar className="mr-3 h-4 w-4" />
              Lịch đặt sân
            </Button>
            <Button
              variant="default"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/reviews")}
            >
              <Star className="mr-3 h-4 w-4" />
              Đánh giá của khách
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/complaints")}
            >
              <AlertCircle className="mr-3 h-4 w-4" />
              Khiếu nại khách hàng
            </Button>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 p-8 bg-muted/10">
          <h1 className="text-3xl font-bold mb-8">Quản lý đánh giá</h1>

          {/* Filters */}
          <Card className="mb-6">
            <CardContent className="p-4 flex gap-4 items-center flex-wrap">
              <div className="flex-1 min-w-[200px]">
                <Input
                  placeholder="Tìm theo sân, tên khách hoặc nhận xét..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Select value={filterRating} onValueChange={setFilterRating}>
                <SelectTrigger className="w-48">
                  <SelectValue placeholder="Tất cả đánh giá" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả đánh giá</SelectItem>
                  <SelectItem value="5">5 Sao ⭐⭐⭐⭐⭐</SelectItem>
                  <SelectItem value="4">4 Sao ⭐⭐⭐⭐</SelectItem>
                  <SelectItem value="3">3 Sao ⭐⭐⭐</SelectItem>
                  <SelectItem value="2">2 Sao ⭐⭐</SelectItem>
                  <SelectItem value="1">1 Sao ⭐</SelectItem>
                </SelectContent>
              </Select>
            </CardContent>
          </Card>

          {/* Reviews List */}
          <div className="space-y-6">
            {getFilteredReviews().map((review) => (
              <Card key={review.id} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="flex justify-between items-start mb-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-lg text-primary">{review.stadiumName}</span>
                        <Badge variant="outline" className="text-xs">Đơn: {review.bookingId}</Badge>
                      </div>
                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <User className="h-4 w-4" />
                        <span>{review.customerName || "Khách ẩn danh"}</span>
                        <span>•</span>
                        <Clock className="h-4 w-4" />
                        <span>{new Date(review.reviewDate).toLocaleDateString('vi-VN')}</span>
                      </div>
                    </div>

                    <div className="flex gap-1 text-yellow-500">
                      {Array.from({ length: 5 }).map((_, i) => (
                        <Star
                          key={i}
                          className={`h-5 w-5 ${
                            i < review.rating ? "fill-current" : "text-gray-300"
                          }`}
                        />
                      ))}
                    </div>
                  </div>

                  {/* Customer Review Comment */}
                  <div className="bg-muted/30 p-4 rounded-lg border mb-4">
                    <p className="text-sm italic text-foreground/80">"{review.comment}"</p>
                    {review.tags && review.tags.length > 0 && (
                      <div className="flex gap-2 flex-wrap mt-3">
                        {review.tags.map((tag: string, idx: number) => (
                          <Badge key={idx} variant="secondary" className="text-xs">
                            {tag}
                          </Badge>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Owner Response Section */}
                  {review.ownerResponse ? (
                    <div className="bg-primary/5 border border-primary/20 rounded-lg p-4 relative ml-6">
                      <div className="flex justify-between items-center mb-2">
                        <span className="text-xs font-bold text-primary uppercase tracking-wide">Phản hồi của chủ sân:</span>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-xs text-destructive hover:text-destructive/80 p-0 h-auto"
                          onClick={() => handleDeleteReply(review.id)}
                        >
                          Xóa phản hồi
                        </Button>
                      </div>
                      <p className="text-sm text-foreground/90 font-medium">{review.ownerResponse}</p>
                    </div>
                  ) : (
                    <div className="ml-6 space-y-3">
                      <Label className="text-xs font-semibold text-muted-foreground">Nhập phản hồi cho khách hàng:</Label>
                      <div className="flex gap-2">
                        <Textarea
                          placeholder="Viết phản hồi cảm ơn hoặc làm rõ thông tin..."
                          value={replyText[review.id] || ""}
                          onChange={(e) => setReplyText(prev => ({ ...prev, [review.id]: e.target.value }))}
                          rows={2}
                          className="flex-1"
                        />
                        <Button
                          className="bg-primary hover:bg-primary/95 text-white"
                          disabled={!(replyText[review.id] || "").trim()}
                          onClick={() => handleSendReply(review.id)}
                        >
                          <Send className="h-4 w-4 mr-2" />
                          Gửi
                        </Button>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}

            {getFilteredReviews().length === 0 && (
              <Card>
                <CardContent className="p-12 text-center text-muted-foreground">
                  Không tìm thấy đánh giá nào.
                </CardContent>
              </Card>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}

export default OwnerReviewsPage;
