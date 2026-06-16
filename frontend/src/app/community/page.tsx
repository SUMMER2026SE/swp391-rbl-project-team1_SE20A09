'use client'

import { useState, useEffect } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { toast } from "sonner";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Plus,
  Search,
  Calendar,
  MapPin,
  Users,
  TrendingUp,
  Loader2,
  Clock,
  DollarSign
} from "lucide-react";
import { 
  getActiveMatches, 
  createMatchRequest, 
  joinMatchRequest, 
  MatchResponse 
} from "@/lib/api/matchmaking";
import { getSportTypes, searchStadiums, StadiumResponse } from "@/lib/api/stadium";

function MatchRequestFeedPage() {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showJoinDialog, setShowJoinDialog] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<MatchResponse | null>(null);

  // Lists from APIs
  const [matchRequests, setMatchRequests] = useState<MatchResponse[]>([]);
  const [stadiums, setStadiums] = useState<StadiumResponse[]>([]);
  const [sportTypes, setSportTypes] = useState<{ sportTypeId: number; sportName: string }[]>([]);
  const [loadingFeed, setLoadingFeed] = useState(true);

  // Form states
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [sportTypeId, setSportTypeId] = useState("");
  const [skillLevel, setSkillLevel] = useState<"BEGINNER" | "INTERMEDIATE" | "ADVANCED" | "">("");
  const [playDate, setPlayDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [stadiumId, setStadiumId] = useState("");
  const [maxPlayers, setMaxPlayers] = useState("");
  const [splitPrice, setSplitPrice] = useState(false);
  const [pricePerPlayer, setPricePerPlayer] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Join match states
  const [joinNote, setJoinNote] = useState("");
  const [submittingJoin, setSubmittingJoin] = useState(false);

  // Search/Filters states
  const [searchKeyword, setSearchKeyword] = useState("");
  const [filterSport, setFilterSport] = useState("all");
  const [filterLevel, setFilterLevel] = useState("all-level");

  // Fetch match requests
  const fetchFeed = async () => {
    try {
      setLoadingFeed(true);
      const data = await getActiveMatches(0, 50);
      setMatchRequests(data.content);
    } catch (err: any) {
      toast.error(err.message || "Không thể tải danh sách kèo ghép.");
    } finally {
      setLoadingFeed(false);
    }
  };

  // Fetch dropdown data
  const fetchDropdowns = async () => {
    try {
      const [sportsData, stadiumsData] = await Promise.all([
        getSportTypes(),
        searchStadiums({ size: 100 })
      ]);
      setSportTypes(sportsData);
      setStadiums(stadiumsData.content);
    } catch (err: any) {
      console.error("Lỗi khi tải dữ liệu cấu hình sân và môn học:", err);
    }
  };

  useEffect(() => {
    fetchFeed();
    fetchDropdowns();
  }, []);

  const getSkillLevelLabel = (level: string) => {
    const config = {
      BEGINNER: "Mới bắt đầu",
      INTERMEDIATE: "Trung bình",
      ADVANCED: "Nâng cao",
    };
    return config[level as keyof typeof config] || level;
  };

  const getSkillLevelBadge = (level: string) => {
    const config = {
      BEGINNER: "bg-emerald-50 text-emerald-700 border-emerald-200/50 hover:bg-emerald-50",
      INTERMEDIATE: "bg-blue-50 text-blue-700 border-blue-200/50 hover:bg-blue-50",
      ADVANCED: "bg-purple-50 text-purple-700 border-purple-200/50 hover:bg-purple-50",
    };
    return config[level as keyof typeof config] || "bg-slate-50 text-slate-700";
  };

  const handleCreateMatch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !sportTypeId || !skillLevel || !playDate || !startTime || !endTime || !stadiumId || !maxPlayers) {
      toast.error("Vui lòng điền đầy đủ các thông tin bắt buộc (*)");
      return;
    }

    if (startTime >= endTime) {
      toast.error("Giờ bắt đầu phải trước giờ kết thúc!");
      return;
    }

    try {
      setSubmitting(true);
      await createMatchRequest({
        title,
        description,
        stadiumId: Number(stadiumId),
        sportTypeId: Number(sportTypeId),
        playDate,
        startTime: startTime.length === 5 ? `${startTime}:00` : startTime,
        endTime: endTime.length === 5 ? `${endTime}:00` : endTime,
        maxPlayers: Number(maxPlayers),
        skillLevel: skillLevel as "BEGINNER" | "INTERMEDIATE" | "ADVANCED",
        splitPrice,
        pricePerPlayer: splitPrice ? Number(pricePerPlayer) : undefined,
      });

      toast.success("Đã tạo lời mời ghép kèo thành công!");
      setShowCreateDialog(false);
      // Reset form fields
      setTitle("");
      setDescription("");
      setSportTypeId("");
      setSkillLevel("");
      setPlayDate("");
      setStartTime("");
      setEndTime("");
      setStadiumId("");
      setMaxPlayers("");
      setSplitPrice(false);
      setPricePerPlayer("");
      
      // Reload match requests
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi tạo kèo ghép.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleJoinMatch = async () => {
    if (!selectedRequest) return;
    try {
      setSubmittingJoin(true);
      await joinMatchRequest(selectedRequest.matchId, joinNote);
      toast.success("Gửi yêu cầu tham gia kèo thành công! Đang chờ chủ kèo phê duyệt.");
      setShowJoinDialog(false);
      setJoinNote("");
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi gửi yêu cầu tham gia.");
    } finally {
      setSubmittingJoin(false);
    }
  };

  // Filter local list based on user search
  const filteredRequests = matchRequests.filter((req) => {
    const matchesKeyword =
      req.title.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      (req.description && req.description.toLowerCase().includes(searchKeyword.toLowerCase())) ||
      req.stadiumName.toLowerCase().includes(searchKeyword.toLowerCase());

    const matchesSport = filterSport === "all" || req.sportName.toLowerCase() === filterSport.toLowerCase();
    const matchesLevel = filterLevel === "all-level" || req.skillLevel === filterLevel;

    return matchesKeyword && matchesSport && matchesLevel;
  });

  return (
    <div className="min-h-screen bg-slate-50/50">
      <Header />

      <div className="container mx-auto px-4 py-8 max-w-6xl">
        <div className="flex items-center justify-between mb-8 border-b pb-4">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-800">Cộng đồng Ghép Kèo</h1>
            <p className="text-slate-500 text-sm mt-1">Tìm đối thủ, ghép đội chơi thể thao cùng những người có chung đam mê.</p>
          </div>
          <Button onClick={() => setShowCreateDialog(true)} className="shadow-md shadow-primary/20 gap-2 h-11 px-5 font-bold">
            <Plus className="h-5 w-5" />
            Tạo kèo mới
          </Button>
        </div>

        {/* Filters */}
        <div className="flex gap-4 mb-8 flex-wrap">
          <div className="relative flex-1 min-w-64">
            <Search className="absolute left-3.5 top-3.5 h-4.5 w-4.5 text-slate-400" />
            <Input 
              placeholder="Tìm kiếm theo tiêu đề, sân chơi..." 
              className="pl-10 h-11 border-slate-200 focus:ring-primary/20 bg-white" 
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
            />
          </div>

          <Select value={filterSport} onValueChange={setFilterSport}>
            <SelectTrigger className="w-48 h-11 bg-white border-slate-200 font-medium text-slate-700">
              <SelectValue placeholder="Tất cả môn" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả môn</SelectItem>
              {sportTypes.map((st) => (
                <SelectItem key={st.sportTypeId} value={st.sportName}>
                  {st.sportName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={filterLevel} onValueChange={setFilterLevel}>
            <SelectTrigger className="w-48 h-11 bg-white border-slate-200 font-medium text-slate-700">
              <SelectValue placeholder="Tất cả trình độ" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all-level">Tất cả trình độ</SelectItem>
              <SelectItem value="BEGINNER">Mới bắt đầu</SelectItem>
              <SelectItem value="INTERMEDIATE">Trung bình</SelectItem>
              <SelectItem value="ADVANCED">Nâng cao</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Match Requests Feed */}
        {loadingFeed ? (
          <div className="flex flex-col items-center justify-center py-24 text-slate-400">
            <Loader2 className="w-10 h-10 animate-spin text-primary/50 mb-3" />
            <p className="text-sm font-medium tracking-wide">Đang đồng bộ danh sách kèo chơi...</p>
          </div>
        ) : filteredRequests.length === 0 ? (
          <div className="text-center py-20 bg-white rounded-xl border border-slate-200 shadow-sm">
            <Users className="w-14 h-14 text-slate-200 mx-auto mb-4" />
            <h3 className="text-lg font-bold text-slate-700 mb-1">Chưa có kèo nào khả dụng</h3>
            <p className="text-slate-400 text-sm max-w-sm mx-auto">
              Không tìm thấy kèo chơi nào phù hợp với bộ lọc hoặc hiện tại chưa có lời mời nào. Bạn hãy là người đầu tiên tạo kèo nhé!
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6">
            {filteredRequests.map((request) => (
              <Card key={request.matchId} className="hover:shadow-md border-slate-200/80 transition-shadow bg-white overflow-hidden">
                <CardContent className="p-6">
                  <div className="flex gap-4 items-start">
                    {/* Host Avatar */}
                    <Avatar className="h-12 w-12 border border-slate-100 shadow-xs">
                      <AvatarImage src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(request.hostName)}`} />
                      <AvatarFallback>{request.hostName[0]}</AvatarFallback>
                    </Avatar>

                    <div className="flex-1">
                      {/* Header */}
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <div className="flex items-center gap-3">
                            <span className="font-bold text-slate-800 text-base">{request.hostName}</span>
                            <span className="text-slate-400 text-xs">• {new Date(request.createdAt).toLocaleDateString("vi-VN")}</span>
                          </div>
                          <h2 className="text-lg font-bold text-slate-800 mt-1 mb-2">{request.title}</h2>
                          <div className="flex gap-2">
                            <Badge variant="outline" className="text-slate-600 bg-slate-50 border-slate-200/85">{request.sportName}</Badge>
                            <Badge className={`font-semibold border ${getSkillLevelBadge(request.skillLevel)}`}>
                              {getSkillLevelLabel(request.skillLevel)}
                            </Badge>
                          </div>
                        </div>
                        <Button
                          disabled={request.currentPlayers >= request.maxPlayers}
                          onClick={() => {
                            setSelectedRequest(request);
                            setShowJoinDialog(true);
                          }}
                          className="shadow-sm font-semibold"
                        >
                          {request.currentPlayers >= request.maxPlayers ? "Đã Đủ Người" : "Tham gia kèo"}
                        </Button>
                      </div>

                      {/* Description */}
                      <p className="mb-4 text-slate-600 text-sm leading-relaxed">
                        {request.description || "Chưa có mô tả chi tiết."}
                      </p>

                      {/* Details */}
                      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-xs font-semibold text-slate-500 border-t pt-4">
                        <div className="flex items-center">
                          <Calendar className="h-4 w-4 mr-2.5 text-primary" />
                          <span>
                            Ngày đá: <strong className="text-slate-700">{new Date(request.playDate).toLocaleDateString("vi-VN")}</strong>
                          </span>
                        </div>

                        <div className="flex items-center">
                          <Clock className="h-4 w-4 mr-2.5 text-primary" />
                          <span>
                            Giờ chơi: <strong className="text-slate-700">{request.startTime.substring(0, 5)} - {request.endTime.substring(0, 5)}</strong>
                          </span>
                        </div>

                        <div className="flex items-center">
                          <MapPin className="h-4 w-4 mr-2.5 text-primary" />
                          <span className="truncate" title={`${request.stadiumName} (${request.stadiumAddress})`}>
                            Địa điểm: <strong className="text-slate-700">{request.stadiumName}</strong>
                          </span>
                        </div>

                        <div className="flex items-center">
                          <Users className="h-4 w-4 mr-2.5 text-primary" />
                          <span>
                            Sĩ số: <strong className="text-slate-700">{request.currentPlayers}/{request.maxPlayers} thành viên</strong>
                          </span>
                        </div>
                      </div>

                      {/* Price Sharing */}
                      {request.splitPrice && request.pricePerPlayer !== undefined && (
                        <div className="mt-3 flex items-center gap-1.5 text-xs font-semibold text-emerald-600 bg-emerald-50/50 px-2.5 py-1.5 rounded-lg w-fit">
                          <DollarSign className="w-4.5 h-4.5 text-emerald-500" />
                          <span>Chia tiền sân: <strong className="text-emerald-700">{request.pricePerPlayer.toLocaleString("vi-VN")} đ / người</strong></span>
                        </div>
                      )}

                      {/* Progress Bar */}
                      <div className="mt-4">
                        <div className="w-full bg-slate-100 rounded-full h-2">
                          <div
                            className="bg-primary h-2 rounded-full transition-all duration-300"
                            style={{
                              width: `${Math.min(100, (request.currentPlayers / request.maxPlayers) * 100)}%`,
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Create Match Request Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent className="max-w-2xl bg-white border border-slate-200 p-6">
          <DialogHeader className="border-b pb-3 mb-4">
            <DialogTitle className="text-xl font-bold text-slate-800">Tạo lời mời tìm đối thủ</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleCreateMatch} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="title" className="font-bold text-slate-700">Tiêu đề kèo chơi *</Label>
              <Input
                id="title"
                placeholder="VD: Cần tìm 3 bạn giao lưu bóng đá sân Hoa Lư"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="border-slate-200"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="sport" className="font-bold text-slate-700">Môn thể thao *</Label>
                <Select value={sportTypeId} onValueChange={setSportTypeId}>
                  <SelectTrigger id="sport" className="border-slate-200">
                    <SelectValue placeholder="Chọn môn thể thao" />
                  </SelectTrigger>
                  <SelectContent>
                    {sportTypes.map((st) => (
                      <SelectItem key={st.sportTypeId} value={st.sportTypeId.toString()}>
                        {st.sportName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="skill" className="font-bold text-slate-700">Trình độ yêu cầu *</Label>
                <Select value={skillLevel} onValueChange={(val: any) => setSkillLevel(val)}>
                  <SelectTrigger id="skill" className="border-slate-200">
                    <SelectValue placeholder="Chọn trình độ" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="BEGINNER">Mới bắt đầu</SelectItem>
                    <SelectItem value="INTERMEDIATE">Trung bình</SelectItem>
                    <SelectItem value="ADVANCED">Nâng cao</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date" className="font-bold text-slate-700">Ngày chơi *</Label>
                <Input
                  id="date"
                  type="date"
                  min={new Date().toISOString().split("T")[0]}
                  value={playDate}
                  onChange={(e) => setPlayDate(e.target.value)}
                  className="border-slate-200"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="startTime" className="font-bold text-slate-700">Giờ bắt đầu *</Label>
                <Input
                  id="startTime"
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="border-slate-200"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endTime" className="font-bold text-slate-700">Giờ kết thúc *</Label>
                <Input
                  id="endTime"
                  type="time"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="border-slate-200"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="venue-select" className="font-bold text-slate-700">Sân chơi *</Label>
                <Select value={stadiumId} onValueChange={setStadiumId}>
                  <SelectTrigger id="venue-select" className="border-slate-200">
                    <SelectValue placeholder="Chọn sân bóng" />
                  </SelectTrigger>
                  <SelectContent>
                    {stadiums.map((st) => (
                      <SelectItem key={st.stadiumId} value={st.stadiumId.toString()}>
                        {st.stadiumName} ({st.address})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="players" className="font-bold text-slate-700">Số người chơi tối đa *</Label>
                <Input
                  id="players"
                  type="number"
                  min="2"
                  max="50"
                  placeholder="VD: 10"
                  value={maxPlayers}
                  onChange={(e) => setMaxPlayers(e.target.value)}
                  className="border-slate-200"
                />
              </div>
            </div>

            {/* Split Price Settings */}
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <Label htmlFor="split-price" className="font-bold text-slate-700 block">Chia tiền sân (Split Price)</Label>
                  <span className="text-[11px] text-slate-500 font-medium">Bật nếu bạn muốn những người tham gia cùng chia sẻ chi phí sân chơi.</span>
                </div>
                <Switch 
                  id="split-price" 
                  checked={splitPrice} 
                  onCheckedChange={setSplitPrice} 
                />
              </div>

              {splitPrice && (
                <div className="space-y-2">
                  <Label htmlFor="price-per-player" className="font-bold text-slate-700 text-xs">Chi phí mỗi người chơi (đ) *</Label>
                  <Input
                    id="price-per-player"
                    type="number"
                    min="1000"
                    placeholder="VD: 50000"
                    value={pricePerPlayer}
                    onChange={(e) => setPricePerPlayer(e.target.value)}
                    className="border-slate-200 bg-white"
                  />
                </div>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="desc" className="font-bold text-slate-700">Mô tả thêm</Label>
              <Textarea
                id="desc"
                placeholder="VD: Nhóm thân thiện, đá giao lưu học hỏi, mặc áo màu đỏ nhé cả nhà..."
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="border-slate-200"
              />
            </div>

            <div className="flex gap-3 border-t pt-4">
              <Button
                type="button"
                variant="outline"
                className="flex-1 font-semibold"
                onClick={() => setShowCreateDialog(false)}
                disabled={submitting}
              >
                Hủy
              </Button>
              <Button type="submit" className="flex-1 font-bold shadow-md shadow-primary/10" disabled={submitting}>
                {submitting && <Loader2 className="w-4.5 h-4.5 animate-spin mr-1.5" />}
                Tạo lời mời
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* Join Match Request Dialog */}
      <Dialog open={showJoinDialog} onOpenChange={setShowJoinDialog}>
        <DialogContent className="bg-white border border-slate-200 p-6">
          <DialogHeader className="border-b pb-3 mb-4">
            <DialogTitle className="text-xl font-bold text-slate-800">Xin tham gia kèo chơi</DialogTitle>
          </DialogHeader>

          {selectedRequest && (
            <div className="space-y-4">
              <Card className="border-slate-200 bg-slate-50/50">
                <CardContent className="p-4">
                  <h3 className="font-bold text-slate-800 text-base mb-3">{selectedRequest.title}</h3>
                  <div className="space-y-2.5 text-xs font-semibold text-slate-500">
                    <div>
                      Môn thể thao: <strong className="text-slate-700">{selectedRequest.sportName}</strong>
                    </div>
                    <div>
                      Thời gian: <strong className="text-slate-700">{new Date(selectedRequest.playDate).toLocaleDateString("vi-VN")} ({selectedRequest.startTime.substring(0, 5)} - {selectedRequest.endTime.substring(0, 5)})</strong>
                    </div>
                    <div>
                      Sân vận động: <strong className="text-slate-700">{selectedRequest.stadiumName}</strong>
                    </div>
                    <div>
                      Sĩ số: <strong className="text-slate-700">{selectedRequest.currentPlayers}/{selectedRequest.maxPlayers}</strong>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <div className="space-y-2">
                <Label htmlFor="join-note" className="font-bold text-slate-700">Lời nhắn tới chủ kèo (không bắt buộc)</Label>
                <Textarea
                  id="join-note"
                  placeholder="VD: Mình đá ở vị trí tiền đạo, trình độ trung bình, cho mình tham gia với nhé..."
                  rows={3}
                  value={joinNote}
                  onChange={(e) => setJoinNote(e.target.value)}
                  className="border-slate-200"
                />
              </div>

              <div className="flex gap-3 border-t pt-4">
                <Button
                  type="button"
                  variant="outline"
                  className="flex-1 font-semibold"
                  onClick={() => setShowJoinDialog(false)}
                  disabled={submittingJoin}
                >
                  Hủy
                </Button>
                <Button onClick={handleJoinMatch} className="flex-1 font-bold shadow-md shadow-primary/10" disabled={submittingJoin}>
                  {submittingJoin && <Loader2 className="w-4.5 h-4.5 animate-spin mr-1.5" />}
                  Gửi yêu cầu tham gia
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      <Footer />
    </div>
  );
}

export default MatchRequestFeedPage;
