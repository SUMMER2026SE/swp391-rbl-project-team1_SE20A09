"use client";

import { useState, useEffect } from "react";
import { useSession } from "next-auth/react";
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
  DollarSign,
  Check,
  X,
  ShieldAlert,
} from "lucide-react";
import {
  getActiveMatches,
  createMatchRequest,
  joinMatchRequest,
  getJoinRequests,
  approveJoinRequest,
  rejectJoinRequest,
  MatchResponse,
  JoinRequestResponse,
} from "@/lib/api/matchmaking";
import {
  getSportTypes,
  searchStadiums,
  StadiumResponse,
} from "@/lib/api/stadium";

function MatchRequestFeedPage() {
  const { data: session } = useSession();

  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showJoinDialog, setShowJoinDialog] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<MatchResponse | null>(
    null,
  );

  // Host manage dialog states
  const [showManageDialog, setShowManageDialog] = useState(false);
  const [selectedManageMatch, setSelectedManageMatch] = useState<MatchResponse | null>(null);
  const [joinRequests, setJoinRequests] = useState<JoinRequestResponse[]>([]);
  const [loadingRequests, setLoadingRequests] = useState(false);
  const [actioningId, setActioningId] = useState<number | null>(null);

  // Lists from APIs
  const [matchRequests, setMatchRequests] = useState<MatchResponse[]>([]);
  const [stadiums, setStadiums] = useState<StadiumResponse[]>([]);
  const [sportTypes, setSportTypes] = useState<
    { sportTypeId: number; sportName: string }[]
  >([]);
  const [loadingFeed, setLoadingFeed] = useState(true);

  // Form states
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [sportTypeId, setSportTypeId] = useState("");
  const [skillLevel, setSkillLevel] = useState<
    "BEGINNER" | "INTERMEDIATE" | "ADVANCED" | ""
  >("");
  const [playDate, setPlayDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [stadiumId, setStadiumId] = useState("");
  const [maxPlayers, setMaxPlayers] = useState("");
  const [splitPrice, setSplitPrice] = useState(false);
  const [pricePerPlayer, setPricePerPlayer] = useState("");
  const [matchingType, setMatchingType] = useState<
    "INDIVIDUAL" | "TEAM_VS_TEAM"
  >("INDIVIDUAL");
  const [submitting, setSubmitting] = useState(false);

  // Join match states
  const [joinNote, setJoinNote] = useState("");
  const [submittingJoin, setSubmittingJoin] = useState(false);

  // Search/Filters states
  const [searchKeyword, setSearchKeyword] = useState("");
  const [filterSport, setFilterSport] = useState("all");
  const [filterLevel, setFilterLevel] = useState("all-level");
  const [filterMatchingType, setFilterMatchingType] = useState("all");

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
        searchStadiums({ size: 100 }),
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
      BEGINNER:
        "bg-emerald-50 text-emerald-700 border-emerald-200/50 hover:bg-emerald-50",
      INTERMEDIATE:
        "bg-blue-50 text-blue-700 border-blue-200/50 hover:bg-blue-50",
      ADVANCED:
        "bg-purple-50 text-purple-700 border-purple-200/50 hover:bg-purple-50",
    };
    return config[level as keyof typeof config] || "bg-slate-50 text-slate-700";
  };

  const handleCreateMatch = async (e: React.FormEvent) => {
    e.preventDefault();
    const isIndividual = matchingType === "INDIVIDUAL";
    if (
      !title ||
      !sportTypeId ||
      !skillLevel ||
      !playDate ||
      !startTime ||
      !endTime ||
      !stadiumId ||
      (isIndividual && !maxPlayers)
    ) {
      toast.error("Vui lòng điền đầy đủ các thông tin bắt buộc (*)");
      return;
    }

    if (startTime >= endTime) {
      toast.error("Giờ bắt đầu phải trước giờ kết thúc!");
      return;
    }

    try {
      setSubmitting(true);
      const finalMaxPlayers = isIndividual ? Number(maxPlayers) : 2;
      await createMatchRequest({
        title,
        description,
        stadiumId: Number(stadiumId),
        sportTypeId: Number(sportTypeId),
        playDate,
        startTime: startTime.length === 5 ? `${startTime}:00` : startTime,
        endTime: endTime.length === 5 ? `${endTime}:00` : endTime,
        maxPlayers: finalMaxPlayers,
        skillLevel: skillLevel as "BEGINNER" | "INTERMEDIATE" | "ADVANCED",
        splitPrice,
        pricePerPlayer: splitPrice ? Number(pricePerPlayer) : undefined,
        matchingType,
      });

      toast.success(
        matchingType === "TEAM_VS_TEAM"
          ? "Đã tạo lời mời cáp kèo đội thành công!"
          : "Đã tạo lời mời ghép kèo thành công!",
      );
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
      setMatchingType("INDIVIDUAL");

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
    if (selectedRequest.matchingType === "TEAM_VS_TEAM" && !joinNote.trim()) {
      toast.error("Vui lòng nhập Tên đội bóng của bạn!");
      return;
    }
    try {
      setSubmittingJoin(true);
      await joinMatchRequest(selectedRequest.matchId, joinNote);
      toast.success(
        "Gửi yêu cầu tham gia kèo thành công! Đang chờ chủ kèo phê duyệt.",
      );
      setShowJoinDialog(false);
      setJoinNote("");
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi gửi yêu cầu tham gia.");
    } finally {
      setSubmittingJoin(false);
    }
  };

  const fetchJoinRequests = async (matchId: number) => {
    try {
      setLoadingRequests(true);
      const data = await getJoinRequests(matchId);
      setJoinRequests(data);
    } catch (err: any) {
      toast.error(err.message || "Không thể tải danh sách đơn đăng ký.");
    } finally {
      setLoadingRequests(false);
    }
  };

  useEffect(() => {
    if (selectedManageMatch && showManageDialog) {
      fetchJoinRequests(selectedManageMatch.matchId);
    }
  }, [selectedManageMatch, showManageDialog]);

  const handleApprove = async (joinId: number) => {
    if (!selectedManageMatch) return;
    try {
      setActioningId(joinId);
      await approveJoinRequest(selectedManageMatch.matchId, joinId);
      toast.success("Đã phê duyệt yêu cầu tham gia thành công!");
      await fetchJoinRequests(selectedManageMatch.matchId);
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi phê duyệt yêu cầu.");
    } finally {
      setActioningId(null);
    }
  };

  const handleReject = async (joinId: number) => {
    if (!selectedManageMatch) return;
    try {
      setActioningId(joinId);
      await rejectJoinRequest(selectedManageMatch.matchId, joinId);
      toast.success("Đã từ chối yêu cầu tham gia.");
      await fetchJoinRequests(selectedManageMatch.matchId);
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi từ chối yêu cầu.");
    } finally {
      setActioningId(null);
    }
  };

  // Filter local list based on user search
  const filteredRequests = matchRequests.filter((req) => {
    const matchesKeyword =
      req.title.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      (req.description &&
        req.description.toLowerCase().includes(searchKeyword.toLowerCase())) ||
      req.stadiumName.toLowerCase().includes(searchKeyword.toLowerCase());

    const matchesSport =
      filterSport === "all" ||
      req.sportName.toLowerCase() === filterSport.toLowerCase();
    const matchesLevel =
      filterLevel === "all-level" || req.skillLevel === filterLevel;
    const matchesMatchingType =
      filterMatchingType === "all" || req.matchingType === filterMatchingType;

    return (
      matchesKeyword && matchesSport && matchesLevel && matchesMatchingType
    );
  });

  return (
    <div className="min-h-screen bg-slate-50/50">
      <Header />

      <div className="container mx-auto px-4 py-8 max-w-6xl">
        <div className="flex items-center justify-between mb-8 border-b pb-4">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-800">
              Cộng đồng Ghép Kèo
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Tìm đối thủ, ghép đội chơi thể thao cùng những người có chung đam
              mê.
            </p>
          </div>
          <Button
            onClick={() => setShowCreateDialog(true)}
            className="shadow-md shadow-primary/20 gap-2 h-11 px-5 font-bold"
          >
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

          <Select
            value={filterMatchingType}
            onValueChange={setFilterMatchingType}
          >
            <SelectTrigger className="w-48 h-11 bg-white border-slate-200 font-medium text-slate-700">
              <SelectValue placeholder="Tất cả hình thức" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả hình thức</SelectItem>
              <SelectItem value="INDIVIDUAL">Ghép lẻ</SelectItem>
              <SelectItem value="TEAM_VS_TEAM">Cáp kèo Đội</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Match Requests Feed */}
        {loadingFeed ? (
          <div className="flex flex-col items-center justify-center py-24 text-slate-400">
            <Loader2 className="w-10 h-10 animate-spin text-primary/50 mb-3" />
            <p className="text-sm font-medium tracking-wide">
              Đang đồng bộ danh sách kèo chơi...
            </p>
          </div>
        ) : filteredRequests.length === 0 ? (
          <div className="text-center py-20 bg-white rounded-xl border border-slate-200 shadow-sm">
            <Users className="w-14 h-14 text-slate-200 mx-auto mb-4" />
            <h3 className="text-lg font-bold text-slate-700 mb-1">
              Chưa có kèo nào khả dụng
            </h3>
            <p className="text-slate-400 text-sm max-w-sm mx-auto">
              Không tìm thấy kèo chơi nào phù hợp với bộ lọc hoặc hiện tại chưa
              có lời mời nào. Bạn hãy là người đầu tiên tạo kèo nhé!
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6">
            {filteredRequests.map((request) => (
              <Card
                key={request.matchId}
                className="hover:shadow-md border-slate-200/80 transition-shadow bg-white overflow-hidden"
              >
                <CardContent className="p-6">
                  <div className="flex gap-4 items-start">
                    {/* Host Avatar */}
                    <Avatar className="h-12 w-12 border border-slate-100 shadow-xs">
                      <AvatarImage
                        src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(request.hostName)}`}
                      />
                      <AvatarFallback>{request.hostName[0]}</AvatarFallback>
                    </Avatar>

                    <div className="flex-1">
                      {/* Header */}
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <div className="flex items-center gap-3">
                            <span className="font-bold text-slate-800 text-base">
                              {request.hostName}
                            </span>
                            <span className="text-slate-400 text-xs">
                              •{" "}
                              {new Date(request.createdAt).toLocaleDateString(
                                "vi-VN",
                              )}
                            </span>
                          </div>
                          <h2 className="text-lg font-bold text-slate-800 mt-1 mb-2">
                            {request.title}
                          </h2>
                          <div className="flex gap-2">
                            <Badge
                              variant="outline"
                              className={
                                request.matchingType === "TEAM_VS_TEAM"
                                  ? "bg-blue-50 text-blue-700 border-blue-200/60 font-semibold"
                                  : "bg-emerald-50 text-emerald-700 border-emerald-200/60 font-semibold"
                              }
                            >
                              {request.matchingType === "TEAM_VS_TEAM"
                                ? "Đội vs Đội"
                                : "Ghép lẻ"}
                            </Badge>
                            <Badge
                              variant="outline"
                              className="text-slate-600 bg-slate-50 border-slate-200/85"
                            >
                              {request.sportName}
                            </Badge>
                            <Badge
                              className={`font-semibold border ${getSkillLevelBadge(request.skillLevel)}`}
                            >
                              {getSkillLevelLabel(request.skillLevel)}
                            </Badge>
                          </div>
                        </div>
                        {session?.user && (session.user as any).userId === request.hostUserId ? (
                          <Button
                            onClick={() => {
                              setSelectedManageMatch(request);
                              setShowManageDialog(true);
                            }}
                            className="bg-amber-500 hover:bg-amber-600 text-white shadow-sm font-semibold gap-1.5"
                          >
                            <Users className="w-4 h-4" />
                            Quản lý đơn
                          </Button>
                        ) : (
                          <Button
                            disabled={
                              request.currentPlayers >=
                              (request.matchingType === "TEAM_VS_TEAM"
                                ? 2
                                : request.maxPlayers)
                            }
                            onClick={() => {
                              setSelectedRequest(request);
                              setShowJoinDialog(true);
                            }}
                            className={`shadow-sm font-semibold ${
                              request.matchingType === "TEAM_VS_TEAM" &&
                              request.currentPlayers < 2
                                ? "bg-blue-600 hover:bg-blue-700 text-white"
                                : ""
                            }`}
                          >
                            {request.currentPlayers >=
                            (request.matchingType === "TEAM_VS_TEAM"
                              ? 2
                              : request.maxPlayers)
                              ? request.matchingType === "TEAM_VS_TEAM"
                                ? "Đã Đủ Đội"
                                : "Đã Đủ Người"
                              : request.matchingType === "TEAM_VS_TEAM"
                                ? "Cáp kèo"
                                : "Tham gia kèo"}
                          </Button>
                        )}
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
                            Ngày đá:{" "}
                            <strong className="text-slate-700">
                              {new Date(request.playDate).toLocaleDateString(
                                "vi-VN",
                              )}
                            </strong>
                          </span>
                        </div>

                        <div className="flex items-center">
                          <Clock className="h-4 w-4 mr-2.5 text-primary" />
                          <span>
                            Giờ chơi:{" "}
                            <strong className="text-slate-700">
                              {request.startTime.substring(0, 5)} -{" "}
                              {request.endTime.substring(0, 5)}
                            </strong>
                          </span>
                        </div>

                        <div className="flex items-center">
                          <MapPin className="h-4 w-4 mr-2.5 text-primary" />
                          <span
                            className="truncate"
                            title={`${request.stadiumName} (${request.stadiumAddress})`}
                          >
                            Địa điểm:{" "}
                            <strong className="text-slate-700">
                              {request.stadiumName}
                            </strong>
                          </span>
                        </div>

                        <div className="flex items-center">
                          <Users className="h-4 w-4 mr-2.5 text-primary" />
                          <span>
                            {request.matchingType === "TEAM_VS_TEAM" ? (
                              <>
                                Trạng thái:{" "}
                                <strong className="text-slate-700">
                                  {request.currentPlayers}/2 đội
                                </strong>
                              </>
                            ) : (
                              <>
                                Sĩ số:{" "}
                                <strong className="text-slate-700">
                                  {request.currentPlayers}/{request.maxPlayers}{" "}
                                  thành viên
                                </strong>
                              </>
                            )}
                          </span>
                        </div>
                      </div>

                      {/* Price Sharing */}
                      {request.splitPrice &&
                        request.pricePerPlayer !== undefined && (
                          <div className="mt-3 flex items-center gap-1.5 text-xs font-semibold text-emerald-600 bg-emerald-50/50 px-2.5 py-1.5 rounded-lg w-fit">
                            <DollarSign className="w-4.5 h-4.5 text-emerald-500" />
                            <span>
                              {request.matchingType === "TEAM_VS_TEAM" ? (
                                <>
                                  Chia đôi tiền sân:{" "}
                                  <strong className="text-emerald-700">
                                    {request.pricePerPlayer.toLocaleString(
                                      "vi-VN",
                                    )}{" "}
                                    đ / đội
                                  </strong>
                                </>
                              ) : (
                                <>
                                  Chia tiền sân:{" "}
                                  <strong className="text-emerald-700">
                                    {request.pricePerPlayer.toLocaleString(
                                      "vi-VN",
                                    )}{" "}
                                    đ / người
                                  </strong>
                                </>
                              )}
                            </span>
                          </div>
                        )}

                      {/* Progress Bar */}
                      <div className="mt-4">
                        <div className="w-full bg-slate-100 rounded-full h-2">
                          <div
                            className={
                              request.matchingType === "TEAM_VS_TEAM"
                                ? "bg-blue-600 h-2 rounded-full transition-all duration-300"
                                : "bg-primary h-2 rounded-full transition-all duration-300"
                            }
                            style={{
                              width: `${Math.min(100, (request.currentPlayers / (request.matchingType === "TEAM_VS_TEAM" ? 2 : request.maxPlayers)) * 100)}%`,
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
            <DialogTitle className="text-xl font-bold text-slate-800">
              Tạo lời mời tìm đối thủ
            </DialogTitle>
          </DialogHeader>

          <form onSubmit={handleCreateMatch} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="title" className="font-bold text-slate-700">
                Tiêu đề kèo chơi *
              </Label>
              <Input
                id="title"
                placeholder="VD: Cần tìm 3 bạn giao lưu bóng đá sân Hoa Lư"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="border-slate-200"
              />
            </div>

            <div className="space-y-2">
              <Label className="font-bold text-slate-700">
                Hình thức ghép kèo *
              </Label>
              <div className="grid grid-cols-2 gap-4">
                <button
                  type="button"
                  onClick={() => setMatchingType("INDIVIDUAL")}
                  className={`flex flex-col items-center justify-center p-3 rounded-xl border text-center transition-all ${
                    matchingType === "INDIVIDUAL"
                      ? "border-primary bg-primary/5 text-primary ring-2 ring-primary/20"
                      : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                  }`}
                >
                  <span className="font-bold text-sm">Ghép người chơi lẻ</span>
                  <span className="text-[10px] opacity-80 mt-0.5">
                    Tìm thêm thành viên cho đủ đội
                  </span>
                </button>
                <button
                  type="button"
                  onClick={() => setMatchingType("TEAM_VS_TEAM")}
                  className={`flex flex-col items-center justify-center p-3 rounded-xl border text-center transition-all ${
                    matchingType === "TEAM_VS_TEAM"
                      ? "border-blue-600 bg-blue-50/50 text-blue-700 ring-2 ring-blue-600/20"
                      : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                  }`}
                >
                  <span className="font-bold text-sm">Cáp kèo Đội vs Đội</span>
                  <span className="text-[10px] opacity-80 mt-0.5">
                    Đội của bạn tìm đội đối thủ thách đấu
                  </span>
                </button>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="sport" className="font-bold text-slate-700">
                  Môn thể thao *
                </Label>
                <Select value={sportTypeId} onValueChange={setSportTypeId}>
                  <SelectTrigger id="sport" className="border-slate-200">
                    <SelectValue placeholder="Chọn môn thể thao" />
                  </SelectTrigger>
                  <SelectContent>
                    {sportTypes.map((st) => (
                      <SelectItem
                        key={st.sportTypeId}
                        value={st.sportTypeId.toString()}
                      >
                        {st.sportName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="skill" className="font-bold text-slate-700">
                  Trình độ yêu cầu *
                </Label>
                <Select
                  value={skillLevel}
                  onValueChange={(val: any) => setSkillLevel(val)}
                >
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
                <Label htmlFor="date" className="font-bold text-slate-700">
                  Ngày chơi *
                </Label>
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
                <Label htmlFor="startTime" className="font-bold text-slate-700">
                  Giờ bắt đầu *
                </Label>
                <Input
                  id="startTime"
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="border-slate-200"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endTime" className="font-bold text-slate-700">
                  Giờ kết thúc *
                </Label>
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
                <Label
                  htmlFor="venue-select"
                  className="font-bold text-slate-700"
                >
                  Sân chơi *
                </Label>
                <Select value={stadiumId} onValueChange={setStadiumId}>
                  <SelectTrigger
                    id="venue-select"
                    className="border-slate-200 w-full overflow-hidden [&>span]:truncate"
                  >
                    <SelectValue placeholder="Chọn sân bóng" />
                  </SelectTrigger>
                  <SelectContent>
                    {stadiums.map((st) => (
                      <SelectItem
                        key={st.stadiumId}
                        value={st.stadiumId.toString()}
                      >
                        {st.stadiumName} ({st.address})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="players" className="font-bold text-slate-700">
                  Số người chơi tối đa *
                </Label>
                {matchingType === "TEAM_VS_TEAM" ? (
                  <div className="h-10 border border-slate-200 bg-slate-50 rounded-md flex items-center px-3 text-xs font-semibold text-slate-500">
                    Mặc định là 2 đội (Chủ nhà & Đối thủ)
                  </div>
                ) : (
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
                )}
              </div>
            </div>

            {/* Split Price Settings */}
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <Label
                    htmlFor="split-price"
                    className="font-bold text-slate-700 block"
                  >
                    {matchingType === "TEAM_VS_TEAM"
                      ? "Chia đôi tiền sân (50/50)"
                      : "Chia tiền sân (Split Price)"}
                  </Label>
                  <span className="text-[11px] text-slate-500 font-medium">
                    {matchingType === "TEAM_VS_TEAM"
                      ? "Bật nếu bạn muốn đội đối thủ cùng chia sẻ một nửa chi phí thuê sân."
                      : "Bật nếu bạn muốn những người tham gia cùng chia sẻ chi phí sân chơi."}
                  </span>
                </div>
                <Switch
                  id="split-price"
                  checked={splitPrice}
                  onCheckedChange={setSplitPrice}
                />
              </div>

              {splitPrice && (
                <div className="space-y-2">
                  <Label
                    htmlFor="price-per-player"
                    className="font-bold text-slate-700 text-xs"
                  >
                    {matchingType === "TEAM_VS_TEAM"
                      ? "Chi phí mỗi đội (đ) *"
                      : "Chi phí mỗi người chơi (đ) *"}
                  </Label>
                  <Input
                    id="price-per-player"
                    type="number"
                    min="1000"
                    placeholder={
                      matchingType === "TEAM_VS_TEAM"
                        ? "VD: 150000"
                        : "VD: 50000"
                    }
                    value={pricePerPlayer}
                    onChange={(e) => setPricePerPlayer(e.target.value)}
                    className="border-slate-200 bg-white"
                  />
                </div>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="desc" className="font-bold text-slate-700">
                Mô tả thêm
              </Label>
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
              <Button
                type="submit"
                className="flex-1 font-bold shadow-md shadow-primary/10"
                disabled={submitting}
              >
                {submitting && (
                  <Loader2 className="w-4.5 h-4.5 animate-spin mr-1.5" />
                )}
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
            <DialogTitle className="text-xl font-bold text-slate-800">
              Xin tham gia kèo chơi
            </DialogTitle>
          </DialogHeader>

          {selectedRequest && (
            <div className="space-y-4">
              <Card className="border-slate-200 bg-slate-50/50">
                <CardContent className="p-4">
                  <h3 className="font-bold text-slate-800 text-base mb-3">
                    {selectedRequest.title}
                  </h3>
                  <div className="space-y-2.5 text-xs font-semibold text-slate-500">
                    <div>
                      Môn thể thao:{" "}
                      <strong className="text-slate-700">
                        {selectedRequest.sportName}
                      </strong>
                    </div>
                    <div>
                      Thời gian:{" "}
                      <strong className="text-slate-700">
                        {new Date(selectedRequest.playDate).toLocaleDateString(
                          "vi-VN",
                        )}{" "}
                        ({selectedRequest.startTime.substring(0, 5)} -{" "}
                        {selectedRequest.endTime.substring(0, 5)})
                      </strong>
                    </div>
                    <div>
                      Sân vận động:{" "}
                      <strong className="text-slate-700">
                        {selectedRequest.stadiumName}
                      </strong>
                    </div>
                    <div>
                      {selectedRequest.matchingType === "TEAM_VS_TEAM" ? (
                        <>
                          Trạng thái:{" "}
                          <strong className="text-slate-700">
                            {selectedRequest.currentPlayers}/2 đội
                          </strong>
                        </>
                      ) : (
                        <>
                          Sĩ số:{" "}
                          <strong className="text-slate-700">
                            {selectedRequest.currentPlayers}/
                            {selectedRequest.maxPlayers}
                          </strong>
                        </>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>

              <div className="space-y-2">
                <Label htmlFor="join-note" className="font-bold text-slate-700">
                  {selectedRequest.matchingType === "TEAM_VS_TEAM"
                    ? "Tên đội bóng của bạn & lời nhắn *"
                    : "Lời nhắn tới chủ kèo (không bắt buộc)"}
                </Label>
                <Textarea
                  id="join-note"
                  placeholder={
                    selectedRequest.matchingType === "TEAM_VS_TEAM"
                      ? "VD: FC Phủi Quận 1, trình độ trung bình khá, xin cáp kèo đá giao lưu..."
                      : "VD: Mình đá ở vị trí tiền đạo, trình độ trung bình, cho mình tham gia với nhé..."
                  }
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
                <Button
                  onClick={handleJoinMatch}
                  className="flex-1 font-bold shadow-md shadow-primary/10"
                  disabled={submittingJoin}
                >
                  {submittingJoin && (
                    <Loader2 className="w-4.5 h-4.5 animate-spin mr-1.5" />
                  )}
                  Gửi yêu cầu tham gia
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Host Manage Join Requests Dialog */}
      <Dialog open={showManageDialog} onOpenChange={setShowManageDialog}>
        <DialogContent className="max-w-2xl bg-white border border-slate-200 p-6 max-h-[85vh] overflow-y-auto">
          <DialogHeader className="border-b pb-3 mb-4">
            <DialogTitle className="text-xl font-bold text-slate-800">
              Quản lý đơn đăng ký tham gia
            </DialogTitle>
          </DialogHeader>

          {selectedManageMatch && (
            <div className="space-y-5">
              {/* Match Summary Card */}
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80">
                <div className="flex justify-between items-start mb-2">
                  <h3 className="font-extrabold text-slate-800 text-base">
                    {selectedManageMatch.title}
                  </h3>
                  <Badge className={selectedManageMatch.matchStatus === "OPEN" ? "bg-emerald-500 hover:bg-emerald-600" : "bg-slate-500 hover:bg-slate-600"}>
                    {selectedManageMatch.matchStatus === "OPEN" ? "Đang nhận đơn" : "Đã Đóng / Đầy"}
                  </Badge>
                </div>
                <div className="grid grid-cols-2 gap-y-2 gap-x-4 text-xs font-semibold text-slate-500 mt-3">
                  <div>Sân: <span className="text-slate-700">{selectedManageMatch.stadiumName}</span></div>
                  <div>Ngày: <span className="text-slate-700">{new Date(selectedManageMatch.playDate).toLocaleDateString("vi-VN")}</span></div>
                  <div>Thời gian: <span className="text-slate-700">{selectedManageMatch.startTime.substring(0, 5)} - {selectedManageMatch.endTime.substring(0, 5)}</span></div>
                  <div>Sĩ số hiện tại: <span className="text-slate-700">{selectedManageMatch.currentPlayers}/{selectedManageMatch.matchingType === "TEAM_VS_TEAM" ? "2 đội" : selectedManageMatch.maxPlayers}</span></div>
                </div>
              </div>

              {/* Join Requests List */}
              <div className="space-y-4">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Danh sách đơn đăng ký
                </h4>

                {loadingRequests ? (
                  <div className="flex justify-center items-center py-10">
                    <Loader2 className="w-8 h-8 animate-spin text-primary/50" />
                  </div>
                ) : joinRequests.length === 0 ? (
                  <div className="text-center py-12 border border-dashed border-slate-200 rounded-xl bg-slate-50/50">
                    <Users className="w-10 h-10 text-slate-300 mx-auto mb-2.5" />
                    <p className="text-sm font-semibold text-slate-500">Chưa có ai đăng ký tham gia kèo này</p>
                    <p className="text-xs text-slate-400 mt-1">Yêu cầu tham gia từ người dùng khác sẽ hiển thị tại đây.</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {joinRequests.map((req) => (
                      <div
                        key={req.joinId}
                        className="p-4 bg-white border border-slate-200 rounded-xl shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4 hover:border-slate-300 transition-colors"
                      >
                        {/* Requester Info */}
                        <div className="flex gap-3 items-start flex-1 min-w-0">
                          <Avatar className="h-10 w-10 border border-slate-100 shadow-2xs mt-0.5">
                            <AvatarImage
                              src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(req.fullName)}`}
                            />
                            <AvatarFallback>{req.fullName[0]}</AvatarFallback>
                          </Avatar>
                          <div className="flex-1 min-w-0">
                            <div className="font-bold text-slate-800 text-sm truncate">
                              {req.fullName}
                            </div>
                            <div className="text-slate-400 text-[10px] truncate mt-0.5">
                              {req.email} • {new Date(req.createdAt).toLocaleDateString("vi-VN")}
                            </div>
                            {req.message && (
                              <div className="mt-2 bg-slate-50 border border-slate-100 p-2 rounded-lg text-xs text-slate-600 font-medium">
                                <span className="font-bold text-slate-700">
                                  {selectedManageMatch.matchingType === "TEAM_VS_TEAM" ? "Tên đội bóng: " : "Lời nhắn: "}
                                </span>
                                {req.message}
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Status / Actions */}
                        <div className="flex items-center gap-2 self-end md:self-center">
                          {req.requestStatus === "PENDING" ? (
                            <>
                              <Button
                                size="sm"
                                onClick={() => handleReject(req.joinId)}
                                disabled={actioningId !== null}
                                variant="outline"
                                className="border-rose-200 text-rose-600 hover:bg-rose-50/50 hover:text-rose-700 font-semibold text-xs h-9 px-3 gap-1"
                              >
                                {actioningId === req.joinId ? (
                                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                ) : (
                                  <X className="w-3.5 h-3.5" />
                                )}
                                Từ chối
                              </Button>
                              <Button
                                size="sm"
                                onClick={() => handleApprove(req.joinId)}
                                disabled={actioningId !== null}
                                className="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs h-9 px-3 gap-1"
                              >
                                {actioningId === req.joinId ? (
                                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                ) : (
                                  <Check className="w-3.5 h-3.5" />
                                )}
                                Phê duyệt
                              </Button>
                            </>
                          ) : (
                            <Badge
                              variant="outline"
                              className={
                                req.requestStatus === "APPROVED"
                                  ? "bg-emerald-50 text-emerald-700 border-emerald-200/60 font-bold px-2.5 py-1 text-xs"
                                  : req.requestStatus === "REJECTED"
                                    ? "bg-rose-50 text-rose-700 border-rose-200/60 font-bold px-2.5 py-1 text-xs"
                                    : "bg-slate-50 text-slate-600 border-slate-200/60 font-bold px-2.5 py-1 text-xs"
                              }
                            >
                              {req.requestStatus === "APPROVED"
                                ? "Đã Phê Duyệt"
                                : req.requestStatus === "REJECTED"
                                  ? "Đã Từ Chối"
                                  : "Đã Hủy"}
                            </Badge>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
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
