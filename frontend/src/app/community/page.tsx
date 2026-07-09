"use client";

import { useState, useEffect } from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import type { Session } from "next-auth";
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
  ChevronRight,
  MessageSquare,
} from "lucide-react";
import {
  getActiveMatches,
  createMatchRequest,
  joinMatchRequest,
  getJoinRequests,
  approveJoinRequest,
  rejectJoinRequest,
  getMyCreatedMatches,
  getMyJoinedRequests,
  cancelMatchRequest,
  MatchResponse,
  JoinRequestResponse,
} from "@/lib/api/matchmaking";
import {
  getSportTypes,
  searchStadiums,
  StadiumResponse,
} from "@/lib/api/stadium";
import { useConfirm } from "@/hooks/useConfirm";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { chatUrl, createContextualConversation } from "@/lib/contextual-chat";
import { getMatchConversation } from "@/lib/chat-api";

// Helper functions for MatchStatus display
const isMatchEnded = (playDate: string, endTime: string) => {
  if (!playDate || !endTime) return false;
  const endDateTime = new Date(`${playDate}T${endTime}`);
  return new Date() > endDateTime;
};

const isMatchInProgress = (playDate: string, startTime: string, endTime: string) => {
  if (!playDate || !startTime || !endTime) return false;
  const startDateTime = new Date(`${playDate}T${startTime}`);
  const endDateTime = new Date(`${playDate}T${endTime}`);
  const now = new Date();
  return now >= startDateTime && now <= endDateTime;
};

const getMatchDisplayStatus = (m: MatchResponse) => {
  if (m.matchStatus === "CANCELLED") return "Đã hủy";
  if (isMatchEnded(m.playDate, m.endTime)) return "Đã kết thúc";
  if (isMatchInProgress(m.playDate, m.startTime, m.endTime)) return "Đang diễn ra";
  if (m.matchStatus === "OPEN") return "Mở";
  if (m.matchStatus === "FULL") return "Đầy";
  return m.matchStatus;
};

const getMatchStatusBadgeClass = (m: MatchResponse) => {
  if (m.matchStatus === "CANCELLED") return "bg-rose-500 hover:bg-rose-600";
  if (isMatchEnded(m.playDate, m.endTime)) return "bg-slate-500 hover:bg-slate-600";
  if (isMatchInProgress(m.playDate, m.startTime, m.endTime)) return "bg-blue-500 hover:bg-blue-600";
  if (m.matchStatus === "OPEN") return "bg-emerald-500 hover:bg-emerald-600";
  if (m.matchStatus === "FULL") return "bg-amber-500 hover:bg-amber-600";
  return "bg-slate-400 hover:bg-slate-500";
};

function MatchRequestFeedPage() {
  const router = useRouter();
  const { data: session } = useSession();
  const { isOpen, options, confirm, close, execute, isLoading: confirming } = useConfirm();

  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showJoinDialog, setShowJoinDialog] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<MatchResponse | null>(
    null,
  );
  const [showJoinedDetailDialog, setShowJoinedDetailDialog] = useState(false);
  const [selectedJoinedRequest, setSelectedJoinedRequest] = useState<JoinRequestResponse | null>(null);
  const [openingChat, setOpeningChat] = useState(false);

  const openJoinedRequestChat = async (group: boolean) => {
    if (!selectedJoinedRequest) return;
    try {
      setOpeningChat(true);
      const result = group
        ? await getMatchConversation(selectedJoinedRequest.matchId)
        : { conversationId: await createContextualConversation(selectedJoinedRequest.hostUserId!, {
            action: 'match_referral', matchId: selectedJoinedRequest.matchId,
            title: selectedJoinedRequest.matchTitle || `Kèo #${selectedJoinedRequest.matchId}`,
            sportName: selectedJoinedRequest.sportName, playDate: selectedJoinedRequest.playDate,
          }) };
      setShowJoinedDetailDialog(false);
      router.push(chatUrl(result.conversationId));
    } catch { toast.error('Không thể mở cuộc trò chuyện'); }
    finally { setOpeningChat(false); }
  };

  // Host manage dialog states
  const [showManageDialog, setShowManageDialog] = useState(false);
  const [selectedManageMatch, setSelectedManageMatch] = useState<MatchResponse | null>(null);
  const [joinRequests, setJoinRequests] = useState<JoinRequestResponse[]>([]);
  const [loadingRequests, setLoadingRequests] = useState(false);
  const [actioningId, setActioningId] = useState<number | null>(null);
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [matchIdToCancel, setMatchIdToCancel] = useState<number | null>(null);
  const [submittingCancel, setSubmittingCancel] = useState(false);

  const handleHostChatCandidate = async (req: JoinRequestResponse) => {
    if (!req.userId || !selectedManageMatch) return;
    try {
      setOpeningChat(true);
      const conversationId = await createContextualConversation(req.userId, {
        action: "match_referral",
        matchId: selectedManageMatch.matchId,
        title: selectedManageMatch.title || `KÃ¨o #${selectedManageMatch.matchId}`,
        sportName: selectedManageMatch.sportName,
        playDate: selectedManageMatch.playDate,
      });
      setShowManageDialog(false);
      router.push(chatUrl(conversationId));
    } catch {
      toast.error("KhÃ´ng thá»ƒ má»Ÿ cuá»™c trÃ² chuyá»‡n");
    } finally {
      setOpeningChat(false);
    }
  };

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

  // Sidebar states
  const [myCreatedMatches, setMyCreatedMatches] = useState<MatchResponse[]>([]);
  const [myJoinedRequests, setMyJoinedRequests] = useState<JoinRequestResponse[]>([]);
  const [loadingSidebar, setLoadingSidebar] = useState(false);

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

  // Fetch sidebar data
  const fetchSidebarData = async () => {
    if (!session?.user) return;
    try {
      setLoadingSidebar(true);
      const [createdRes, joinedRes] = await Promise.all([
        getMyCreatedMatches(),
        getMyJoinedRequests(),
      ]);
      setMyCreatedMatches(createdRes?.content || []);
      setMyJoinedRequests(joinedRes?.content || []);
    } catch (err) {
      console.error("Lỗi khi tải dữ liệu sidebar:", err);
    } finally {
      setLoadingSidebar(false);
    }
  };

  const handleExecuteCancelMatch = async () => {
    if (!matchIdToCancel) return;
    try {
      setSubmittingCancel(true);
      await cancelMatchRequest(matchIdToCancel, cancelReason);
      toast.success("Hủy kèo ghép thành công.");
      setShowCancelDialog(false);
      setShowManageDialog(false);
      fetchFeed();
      fetchSidebarData();
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi hủy kèo.");
    } finally {
      setSubmittingCancel(false);
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

  useEffect(() => {
    if (session?.user) {
      fetchSidebarData();
    } else {
      setMyCreatedMatches([]);
      setMyJoinedRequests([]);
    }
  }, [session]);

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
        "bg-emerald-50/40 text-emerald-600 border-emerald-100 hover:bg-emerald-50/40",
      INTERMEDIATE:
        "bg-blue-50/40 text-blue-600 border-blue-100 hover:bg-blue-50/40",
      ADVANCED:
        "bg-purple-50/40 text-purple-600 border-purple-100 hover:bg-purple-50/40",
    };
    return config[level as keyof typeof config] || "bg-slate-50/40 text-slate-500 border-slate-100";
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
      fetchSidebarData();
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
        "Gửi yêu cầu tham gia kèo thành công! Đang chờ Host phê duyệt.",
      );
      setShowJoinDialog(false);
      setJoinNote("");
      fetchFeed();
      fetchSidebarData();
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
      fetchSidebarData();
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
      fetchSidebarData();
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

        <div className="flex gap-6 items-start">
          {/* ── LEFT COLUMN: Feed ─────────────────────── */}
          <div className="flex-1 min-w-0">
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

                        <div className="flex-1 min-w-0">
                          {/* Header: Host details & actions */}
                          <div className="flex items-start justify-between gap-4 mb-3">
                            <div className="min-w-0">
                              <div className="flex items-center gap-2 flex-wrap text-slate-400 text-xs">
                                <span className="font-bold text-slate-700 text-sm">
                                  {request.hostName}
                                </span>
                                <span>•</span>
                                <span>
                                  {new Date(request.createdAt).toLocaleDateString("vi-VN")}
                                </span>
                              </div>
                              <h2 className="text-xl font-extrabold text-slate-900 tracking-tight mt-1 hover:text-primary transition-colors">
                                {request.title}
                              </h2>
                            </div>
                            
                            {session?.user && (session.user as Session["user"])?.userId === request.hostUserId ? (
                              <Button
                                onClick={() => {
                                  setSelectedManageMatch(request);
                                  setShowManageDialog(true);
                                }}
                                className="bg-amber-500 hover:bg-amber-600 text-white shadow-sm font-semibold gap-1.5 shrink-0"
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
                                className={`shadow-sm font-semibold shrink-0 ${
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

                          {/* Light Tags Row (Môn, Trình độ, Hình thức) */}
                          <div className="flex flex-wrap gap-2 mb-3.5">
                            <Badge
                              variant="outline"
                              className={`text-[10px] font-medium border border-slate-100/80 px-2 py-0.5 rounded-md ${
                                request.matchingType === "TEAM_VS_TEAM"
                                  ? "bg-blue-50/40 text-blue-600"
                                  : "bg-emerald-50/40 text-emerald-600"
                              }`}
                            >
                              {request.matchingType === "TEAM_VS_TEAM"
                                ? "Đội vs Đội"
                                : "Ghép lẻ"}
                            </Badge>
                            <Badge
                              variant="outline"
                              className="text-[10px] font-medium bg-slate-50/50 text-slate-500 border-slate-100/80 px-2 py-0.5 rounded-md"
                            >
                              {request.sportName}
                            </Badge>
                            <Badge
                              variant="outline"
                              className={`text-[10px] font-medium border ${getSkillLevelBadge(request.skillLevel)} px-2 py-0.5 rounded-md`}
                            >
                              {getSkillLevelLabel(request.skillLevel)}
                            </Badge>
                          </div>

                          {/* Description */}
                          <p className="mb-4 text-slate-600 text-sm leading-relaxed">
                            {request.description || "Chưa có mô tả chi tiết."}
                          </p>

                          {/* Details Metadata (Highlighted Time & Venue) */}
                          <div className="flex flex-wrap gap-x-4 gap-y-2.5 text-xs font-semibold text-slate-500 border-t border-slate-100 pt-4">
                            <div className="flex items-center text-primary-600 bg-primary/5 px-2.5 py-1 rounded-md">
                              <Calendar className="h-3.5 w-3.5 mr-1.5 text-primary" />
                              <span>
                                {new Date(request.playDate).toLocaleDateString("vi-VN")}
                              </span>
                            </div>

                            <div className="flex items-center text-blue-600 bg-blue-50/50 px-2.5 py-1 rounded-md">
                              <Clock className="h-3.5 w-3.5 mr-1.5 text-blue-500" />
                              <span>
                                {request.startTime.substring(0, 5)} - {request.endTime.substring(0, 5)}
                              </span>
                            </div>

                            <div className="flex items-center text-slate-600 bg-slate-50 px-2.5 py-1 rounded-md max-w-xs md:max-w-sm truncate" title={`${request.stadiumName} (${request.stadiumAddress})`}>
                              <MapPin className="h-3.5 w-3.5 mr-1.5 text-slate-500" />
                              <span>
                                {request.stadiumName}
                              </span>
                            </div>
                          </div>

                          {/* Capacity / Slots & Progress Bar */}
                          <div className="mt-4 pt-4 border-t border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
                            <div className="flex-1">
                              <div className="flex justify-between items-center text-xs text-slate-500 mb-1.5">
                                <span className="font-semibold text-slate-700 flex items-center gap-1.5">
                                  <Users className="w-4 h-4 text-primary/70" />
                                  {request.matchingType === "TEAM_VS_TEAM" ? (
                                    <>
                                      Trạng thái: <strong className="text-primary font-bold">{request.currentPlayers}/2 đội</strong>
                                    </>
                                  ) : (
                                    <>
                                      Đã ghép: <strong className="text-primary font-bold">{request.currentPlayers}/{request.maxPlayers} thành viên</strong>
                                    </>
                                  )}
                                </span>
                                <span className="text-[10px] font-bold text-slate-400">
                                  {Math.round((request.currentPlayers / (request.matchingType === "TEAM_VS_TEAM" ? 2 : request.maxPlayers)) * 100)}%
                                </span>
                              </div>
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

                            {request.splitPrice && request.pricePerPlayer !== undefined && (
                              <div className="flex items-center gap-1 text-[11px] font-bold text-emerald-600 bg-emerald-50/50 border border-emerald-100/50 px-2.5 py-1.5 rounded-lg shrink-0 w-fit self-start md:self-center">
                                <DollarSign className="w-3.5 h-3.5 text-emerald-500" />
                                <span>
                                  {request.matchingType === "TEAM_VS_TEAM" ? (
                                    <>
                                      Chia đôi: <strong className="text-emerald-700">{request.pricePerPlayer.toLocaleString("vi-VN")} đ/đội</strong>
                                    </>
                                  ) : (
                                    <>
                                      Chia tiền: <strong className="text-emerald-700">{request.pricePerPlayer.toLocaleString("vi-VN")} đ/người</strong>
                                    </>
                                  )}
                                </span>
                              </div>
                            )}
                          </div>

                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>

          {/* ── RIGHT SIDEBAR: Kèo của tôi ─────────── */}
          {session && (
            <aside className="w-80 lg:w-96 shrink-0 space-y-6 sticky top-24 self-start hidden md:block">
              {/* Panel: Kèo tôi tạo */}
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-4 py-3.5 bg-gradient-to-r from-primary/5 to-primary/10 border-b border-slate-100 flex items-center gap-2">
                  <TrendingUp className="w-4.5 h-4.5 text-primary" />
                  <h3 className="text-sm font-bold text-slate-800">Kèo tôi tạo</h3>
                  <span className="ml-auto text-xs font-bold bg-primary/15 text-primary px-2.5 py-0.5 rounded-full">
                    {myCreatedMatches.length}
                  </span>
                </div>
                <div className="p-3 h-[280px] overflow-y-auto space-y-2 flex flex-col">
                  {loadingSidebar ? (
                    <div className="flex-1 flex items-center justify-center">
                      <Loader2 className="w-5 h-5 animate-spin text-primary/50" />
                    </div>
                  ) : myCreatedMatches.length === 0 ? (
                    <div className="flex-1 flex flex-col items-center justify-center text-center p-4">
                      <p className="text-xs text-slate-400">Bạn chưa tạo kèo nào.</p>
                    </div>
                  ) : (
                    myCreatedMatches.map((m) => (
                      <div
                        key={m.matchId}
                        className="p-3 bg-white border border-slate-100 hover:border-primary/30 rounded-xl hover:shadow-xs cursor-pointer transition-all duration-200 group flex items-center justify-between gap-3"
                        title="Click để quản lý đơn đăng ký tham gia"
                        onClick={() => {
                          setSelectedManageMatch(m);
                          setShowManageDialog(true);
                        }}
                      >
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-bold text-slate-800 group-hover:text-primary transition-colors truncate">
                            {m.title}
                          </p>
                          <p className="text-[11px] text-slate-500 mt-1 flex items-center gap-1.5 flex-wrap">
                            <span className="font-semibold text-slate-700">{m.sportName}</span>
                            <span>•</span>
                            <span>{new Date(m.playDate).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" })}</span>
                            <span>•</span>
                            <span>{m.startTime.substring(0, 5)}-{m.endTime.substring(0, 5)}</span>
                          </p>
                          <p className="text-[10px] text-slate-400 mt-1 font-semibold">
                            Sĩ số: {m.currentPlayers}/{m.matchingType === "TEAM_VS_TEAM" ? "2 đội" : `${m.maxPlayers} người`}
                          </p>
                        </div>
                        <div className="flex flex-col items-end gap-2 shrink-0">
                          <Badge
                            variant="outline"
                            className={`text-[9px] px-2 py-0.5 border-0 font-bold text-white shrink-0 ${getMatchStatusBadgeClass(m)}`}
                          >
                            {getMatchDisplayStatus(m)}
                          </Badge>
                          <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-primary group-hover:translate-x-0.5 transition-all" />
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Panel: Đơn tôi đã gửi */}
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-4 py-3.5 bg-gradient-to-r from-blue-50 to-blue-100/30 border-b border-slate-100 flex items-center gap-2">
                  <ShieldAlert className="w-4.5 h-4.5 text-blue-600" />
                  <h3 className="text-sm font-bold text-slate-800">Đơn tôi đã gửi</h3>
                  <span className="ml-auto text-xs font-bold bg-blue-100 text-blue-700 px-2.5 py-0.5 rounded-full">
                    {myJoinedRequests.filter((r) => r.requestStatus === "PENDING").length} chờ
                  </span>
                </div>
                <div className="p-3 h-[280px] overflow-y-auto space-y-2 flex flex-col">
                  {loadingSidebar ? (
                    <div className="flex-1 flex items-center justify-center">
                      <Loader2 className="w-5 h-5 animate-spin text-primary/50" />
                    </div>
                  ) : myJoinedRequests.length === 0 ? (
                    <div className="flex-1 flex flex-col items-center justify-center text-center p-4">
                      <p className="text-xs text-slate-400">Bạn chưa gửi đơn tham gia nào.</p>
                    </div>
                  ) : (
                    myJoinedRequests.map((req) => (
                      <div
                        key={req.joinId}
                        className="p-3 bg-white border border-slate-100 hover:border-primary/30 rounded-xl hover:shadow-xs cursor-pointer transition-all duration-200 group flex items-center justify-between gap-3"
                        title="Click để xem chi tiết kèo và trạng thái đơn"
                        onClick={() => {
                          setSelectedJoinedRequest(req);
                          setShowJoinedDetailDialog(true);
                        }}
                      >
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-bold text-slate-800 group-hover:text-primary transition-colors truncate">
                            {req.matchTitle || `Kèo #${req.matchId}`}
                          </p>
                          {req.sportName && (
                            <p className="text-[11px] text-slate-500 mt-1 flex items-center gap-1.5 flex-wrap">
                              <span className="font-semibold text-slate-700">{req.sportName}</span>
                              <span>•</span>
                              <span>{req.playDate ? new Date(req.playDate).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" }) : ""}</span>
                            </p>
                          )}
                          {req.message && (
                            <p className="text-[11px] text-slate-400 italic truncate mt-1">"{req.message}"</p>
                          )}
                        </div>
                        <div className="flex flex-col items-end gap-2 shrink-0">
                          <Badge
                            variant="outline"
                            className={`text-[9px] px-2 py-0.5 border-0 font-bold text-white shrink-0 ${
                              req.requestStatus === "PENDING"
                                ? "bg-amber-500 hover:bg-amber-600"
                                : req.requestStatus === "APPROVED"
                                ? "bg-emerald-500 hover:bg-emerald-600"
                                : req.requestStatus === "CANCELLED"
                                ? "bg-slate-400 hover:bg-slate-500"
                                : "bg-rose-500 hover:bg-rose-600"
                            }`}
                          >
                            {req.requestStatus === "PENDING"
                              ? "Chờ"
                              : req.requestStatus === "APPROVED"
                              ? "Duyệt"
                              : req.requestStatus === "CANCELLED"
                              ? "Đã hủy"
                              : "Từ chối"}
                          </Badge>
                          <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-primary group-hover:translate-x-0.5 transition-all" />
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </aside>
          )}
        </div>
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
                    : "Lời nhắn tới Host (không bắt buộc)"}
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
        <DialogContent className="w-[95%] sm:max-w-2xl bg-white border border-slate-200 p-5 sm:p-6 max-h-[90vh] flex flex-col overflow-hidden">
          <DialogHeader className="border-b pb-3.5 mb-2">
            <DialogTitle className="text-xl font-bold text-slate-800">
              Quản lý đơn đăng ký tham gia
            </DialogTitle>
          </DialogHeader>

          {selectedManageMatch && (
            <div className="flex-1 overflow-y-auto space-y-5 pr-1 py-2">
              {/* Match Summary Card */}
              <div className="p-4 bg-slate-50 border border-slate-100 rounded-xl">
                <div className="flex justify-between items-start gap-4 mb-3">
                  <h3 className="font-extrabold text-slate-900 text-base leading-snug">
                    {selectedManageMatch.title}
                  </h3>
                  <Badge className={`${getMatchStatusBadgeClass(selectedManageMatch)} text-white border-0`}>
                    {getMatchDisplayStatus(selectedManageMatch)}
                  </Badge>
                </div>
                <div className="grid grid-cols-2 gap-y-2.5 gap-x-6 text-xs font-semibold text-slate-500 border-t border-slate-200/60 pt-3">
                  <div>Sân bóng: <span className="text-slate-800 font-bold">{selectedManageMatch.stadiumName}</span></div>
                  <div>Ngày chơi: <span className="text-slate-800 font-bold">{new Date(selectedManageMatch.playDate).toLocaleDateString("vi-VN")}</span></div>
                  <div>Giờ chơi: <span className="text-slate-800 font-bold">{selectedManageMatch.startTime.substring(0, 5)} - {selectedManageMatch.endTime.substring(0, 5)}</span></div>
                  <div>Sĩ số hiện tại: <span className="text-slate-800 font-bold">{selectedManageMatch.currentPlayers}/{selectedManageMatch.matchingType === "TEAM_VS_TEAM" ? "2 đội" : selectedManageMatch.maxPlayers}</span></div>
                </div>
              </div>

              {/* Join Requests List */}
              <div className="space-y-4">
                <h4 className="text-xs font-extrabold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5" />
                  Danh sách đơn đăng ký ({joinRequests.length})
                </h4>

                {loadingRequests ? (
                  <div className="flex justify-center items-center py-10">
                    <Loader2 className="w-8 h-8 animate-spin text-primary/50" />
                  </div>
                ) : joinRequests.length === 0 ? (
                  <div className="text-center py-12 border border-dashed border-slate-200 rounded-xl bg-slate-50/50">
                    <Users className="w-10 h-10 text-slate-300 mx-auto mb-2.5" />
                    <p className="text-sm font-semibold text-slate-500">Chưa có ai đăng ký tham gia</p>
                    <p className="text-xs text-slate-400 mt-1">Yêu cầu tham gia từ người dùng khác sẽ hiển thị tại đây.</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {joinRequests.map((req) => (
                      <div
                        key={req.joinId}
                        className="p-4 bg-white border border-slate-200 rounded-xl shadow-2xs hover:border-slate-300 transition-all flex flex-col gap-4"
                      >
                        {/* Requester Info */}
                        <div className="flex justify-between items-center gap-3">
                          <div className="flex gap-3 items-center min-w-0">
                          <Avatar className="h-10 w-10 border border-slate-100 shadow-2xs shrink-0">
                            <AvatarImage
                              src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(req.fullName)}`}
                            />
                            <AvatarFallback>{req.fullName[0]}</AvatarFallback>
                          </Avatar>
                          <div className="min-w-0">
                            <div className="font-bold text-slate-800 text-sm truncate">
                              {req.fullName}
                            </div>
                            <div className="text-slate-400 text-[10px] mt-0.5 truncate">
                              {req.email} • Đăng ký: {new Date(req.createdAt).toLocaleDateString("vi-VN")}
                            </div>
                          </div>
                          </div>
                          {req.userId && req.requestStatus === "PENDING" && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              disabled={openingChat}
                              onClick={() => handleHostChatCandidate(req)}
                              className="rounded-full h-8 w-8 hover:bg-slate-100 text-slate-600 cursor-pointer shrink-0"
                              title="Nháº¯n tin liÃªn há»‡"
                            >
                              {openingChat ? (
                                <Loader2 className="h-4 w-4 animate-spin text-emerald-600" />
                              ) : (
                                <MessageSquare className="h-4 w-4 text-emerald-600" />
                              )}
                            </Button>
                          )}
                        </div>

                        {/* Message box */}
                        {req.message && (
                          <div className="bg-slate-50 border border-slate-100/80 p-3 rounded-lg text-xs text-slate-600 leading-relaxed font-medium">
                            <span className="font-bold text-slate-700 block mb-1">
                              {selectedManageMatch.matchingType === "TEAM_VS_TEAM" ? "💼 Tên đội bóng của đối thủ:" : "💬 Lời nhắn:"}
                            </span>
                            "{req.message}"
                          </div>
                        )}

                        {/* Action Buttons: Pushed to bottom right with border separator */}
                        <div className="flex justify-end items-center gap-3 border-t border-slate-100 pt-3">
                          {req.requestStatus === "PENDING" ? (
                            <>
                              <Button
                                size="sm"
                                onClick={() => handleReject(req.joinId)}
                                disabled={actioningId !== null}
                                variant="outline"
                                className="border-rose-200 text-rose-600 hover:bg-rose-50 hover:text-rose-700 font-bold text-xs h-9 px-4 gap-1.5 rounded-lg"
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
                                className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs h-9 px-4 gap-1.5 rounded-lg shadow-sm"
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
                              className={`font-bold px-3 py-1 text-xs border-0 text-white rounded-lg ${
                                req.requestStatus === "APPROVED"
                                  ? "bg-emerald-500"
                                  : req.requestStatus === "REJECTED"
                                    ? "bg-rose-500"
                                    : "bg-slate-400"
                              }`}
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

          {/* Footer close button */}
          <div className="border-t pt-4 flex justify-between items-center">
            {(selectedManageMatch?.matchStatus === "OPEN" ||
              selectedManageMatch?.matchStatus === "FULL") && (
              <Button
                variant="outline"
                className="border-rose-200 text-rose-600 hover:bg-rose-50 hover:text-rose-700 font-bold px-4 h-10 gap-2"
                onClick={() => {
                  setMatchIdToCancel(selectedManageMatch.matchId);
                  setCancelReason("");
                  setShowCancelDialog(true);
                }}
              >
                <ShieldAlert className="w-4 h-4" />
                Hủy kèo ghép
              </Button>
            )}
            <Button
              variant="outline"
              className="font-bold px-6 h-10 border-slate-200 hover:bg-slate-50 text-slate-700"
              onClick={() => setShowManageDialog(false)}
            >
              Đóng cửa sổ
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Joined Request Detail Dialog */}
      <Dialog open={showJoinedDetailDialog} onOpenChange={setShowJoinedDetailDialog}>
        <DialogContent className="w-[95%] sm:max-w-md bg-white border border-slate-200 p-5 sm:p-6 max-h-[90vh] overflow-y-auto">
          <DialogHeader className="border-b pb-3.5 mb-2">
            <DialogTitle className="text-xl font-bold text-slate-800">
              Chi tiết đơn đăng ký tham gia
            </DialogTitle>
          </DialogHeader>

          {selectedJoinedRequest && (
            <div className="space-y-5">
              {/* Match overview */}
              <div className="p-4 bg-slate-50 border border-slate-100 rounded-xl">
                <h3 className="font-extrabold text-slate-900 text-base mb-3 leading-snug">
                  {selectedJoinedRequest.matchTitle || `Kèo #${selectedJoinedRequest.matchId}`}
                </h3>
                <div className="space-y-2 text-xs font-semibold text-slate-500">
                  {selectedJoinedRequest.sportName && (
                    <div className="flex justify-between items-center py-1.5 border-b border-slate-200/50">
                      <span className="text-slate-400">Môn thể thao</span>
                      <strong className="text-slate-800 font-bold">
                        {selectedJoinedRequest.sportName}
                      </strong>
                    </div>
                  )}
                  {selectedJoinedRequest.stadiumName && (
                    <div className="flex justify-between items-center py-1.5 border-b border-slate-200/50">
                      <span className="text-slate-400">Địa điểm</span>
                      <strong className="text-slate-800 font-bold max-w-[200px] truncate" title={selectedJoinedRequest.stadiumName}>
                        {selectedJoinedRequest.stadiumName}
                      </strong>
                    </div>
                  )}
                  {selectedJoinedRequest.playDate && (
                    <div className="flex justify-between items-center py-1.5 border-b border-slate-200/50">
                      <span className="text-slate-400">Thời gian</span>
                      <strong className="text-slate-800 font-bold">
                        {new Date(selectedJoinedRequest.playDate).toLocaleDateString("vi-VN")}{" "}
                        {selectedJoinedRequest.startTime && selectedJoinedRequest.endTime ? (
                          `(${selectedJoinedRequest.startTime.substring(0, 5)} - ${selectedJoinedRequest.endTime.substring(0, 5)})`
                        ) : ""}
                      </strong>
                    </div>
                  )}
                  {selectedJoinedRequest.matchingType && (
                    <div className="flex justify-between items-center py-1.5">
                      <span className="text-slate-400">Hình thức</span>
                      <strong className="text-slate-800 font-bold">
                        {selectedJoinedRequest.matchingType === "TEAM_VS_TEAM" ? "Đội vs Đội" : "Ghép lẻ"}
                      </strong>
                    </div>
                  )}
                </div>
              </div>

              {/* Status details */}
              <div className="space-y-2">
                <span className="text-xs font-extrabold text-slate-400 uppercase tracking-wider block">Trạng thái đơn</span>
                <div className={`p-4 rounded-xl border flex items-start gap-3 ${
                  selectedJoinedRequest.requestStatus === "PENDING"
                    ? "bg-amber-50/50 border-amber-200/60 text-amber-800"
                    : selectedJoinedRequest.requestStatus === "APPROVED"
                    ? "bg-emerald-50/50 border-emerald-200/60 text-emerald-800"
                    : selectedJoinedRequest.requestStatus === "CANCELLED"
                    ? "bg-slate-50 border-slate-200 text-slate-700"
                    : "bg-rose-50/50 border-rose-200/60 text-rose-800"
                }`}>
                  <div className="flex-1">
                    <div className="font-bold text-sm">
                      {selectedJoinedRequest.requestStatus === "PENDING" && "Đang chờ Host phê duyệt"}
                      {selectedJoinedRequest.requestStatus === "APPROVED" && "Đã được phê duyệt tham gia"}
                      {selectedJoinedRequest.requestStatus === "REJECTED" && "Yêu cầu đã bị từ chối"}
                      {selectedJoinedRequest.requestStatus === "CANCELLED" && "Kèo đã bị hủy bởi Host"}
                    </div>
                    <div className="text-xs mt-1 text-slate-500 font-semibold leading-relaxed">
                      {selectedJoinedRequest.requestStatus === "PENDING" && "Host đang xem xét lời mời tham gia của bạn. Vui lòng quay lại kiểm tra sau."}
                      {selectedJoinedRequest.requestStatus === "APPROVED" && "Chúc mừng! Bạn đã trở thành một phần của kèo này. Hãy liên hệ với Host."}
                      {selectedJoinedRequest.requestStatus === "REJECTED" && "Rất tiếc, yêu cầu tham gia của bạn không được phê duyệt lần này. Hãy tìm các kèo khác nhé!"}
                      {selectedJoinedRequest.requestStatus === "CANCELLED" && (
                        <div className="space-y-2.5">
                          <div>Kèo chơi này đã bị hủy bởi người tạo kèo. Bạn vui lòng tìm kiếm các kèo chơi khác nhé!</div>
                          {selectedJoinedRequest.cancelReason && (
                            <div className="p-3 bg-slate-100/90 border border-slate-200/50 rounded-xl text-slate-700 font-bold not-italic">
                              <span className="font-extrabold text-[10px] text-slate-400 block uppercase tracking-wider mb-1">Lý do hủy từ Host:</span>
                              <span className="italic font-medium text-slate-600">&ldquo;{selectedJoinedRequest.cancelReason}&rdquo;</span>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Message sent */}
              {selectedJoinedRequest.message && (
                <div className="space-y-2">
                  <span className="text-xs font-extrabold text-slate-400 uppercase tracking-wider block">
                    {selectedJoinedRequest.matchingType === "TEAM_VS_TEAM" ? "Tên đội bóng của bạn" : "Lời nhắn của bạn"}
                  </span>
                  <div className="p-3 bg-slate-50 border border-slate-100 rounded-lg text-xs font-medium text-slate-600 leading-relaxed italic">
                    "{selectedJoinedRequest.message}"
                  </div>
                </div>
              )}

              {/* Contact info for approved status */}
              {selectedJoinedRequest.requestStatus === "APPROVED" && selectedJoinedRequest.hostName && (
                <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-xl space-y-3">
                  <span className="text-xs font-bold text-emerald-800 uppercase tracking-wider block">📞 Thông tin liên hệ Host</span>
                  <div className="space-y-2 text-xs text-slate-600 font-medium">
                    <div className="flex justify-between items-center">
                      <span>Tên Host:</span>
                      <span className="text-slate-800 font-bold">{selectedJoinedRequest.hostName}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span>Email liên hệ:</span>
                      <span className="text-slate-800 font-bold">{selectedJoinedRequest.hostEmail || "N/A"}</span>
                    </div>
                  </div>
                  <Button type="button" className="w-full" disabled={openingChat}
                    onClick={() => openJoinedRequestChat(true)}>
                    {openingChat ? 'Đang mở...' : 'Vào nhóm chat của kèo'}
                  </Button>
                </div>
              )}

              {selectedJoinedRequest.requestStatus === "PENDING" && selectedJoinedRequest.hostUserId && (
                <Button type="button" variant="outline" className="w-full" disabled={openingChat}
                  onClick={() => openJoinedRequestChat(false)}>
                  {openingChat ? 'Đang mở...' : 'Nhắn tin hỏi Host'}
                </Button>
              )}

              <div className="border-t pt-4 mt-2 flex justify-end">
                <Button
                  type="button"
                  className="w-full font-bold h-10 shadow-sm"
                  onClick={() => setShowJoinedDetailDialog(false)}
                >
                  Đóng
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Host Cancel Match Request Dialog */}
      <Dialog open={showCancelDialog} onOpenChange={setShowCancelDialog}>
        <DialogContent className="max-w-md bg-white border border-slate-200 p-6">
          <DialogHeader className="border-b pb-3 mb-4">
            <DialogTitle className="text-xl font-bold text-slate-800">
              Hủy kèo ghép
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4">
            <div className="p-3.5 bg-rose-50 border border-rose-100 rounded-xl text-xs text-rose-800 flex gap-2.5 items-start">
              <ShieldAlert className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
              <div>
                <div className="font-bold text-sm mb-0.5">Xác nhận hủy kèo chơi này?</div>
                Hành động này không thể hoàn tác. Tất cả người dùng đã được duyệt hoặc đang chờ sẽ nhận được thông báo hủy và trạng thái đơn của họ sẽ tự động chuyển sang đã hủy.
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="cancel-reason" className="font-bold text-slate-700 text-sm">
                Lý do hủy kèo <span className="text-slate-400 font-normal">(không bắt buộc)</span>
              </Label>
              <Textarea
                id="cancel-reason"
                placeholder="VD: Sân bận đột xuất, thời tiết xấu, bận việc cá nhân..."
                rows={3}
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="border-slate-200 text-sm focus:ring-primary/20"
                maxLength={200}
              />
              <p className="text-[10px] text-slate-400 text-right">Tối đa 200 ký tự</p>
            </div>

            <div className="flex gap-3 border-t pt-4">
              <Button
                type="button"
                variant="outline"
                className="flex-1 font-semibold border-slate-200"
                onClick={() => setShowCancelDialog(false)}
                disabled={submittingCancel}
              >
                Quay lại
              </Button>
              <Button
                onClick={handleExecuteCancelMatch}
                disabled={submittingCancel}
                className="flex-1 bg-rose-600 hover:bg-rose-700 text-white font-bold shadow-md shadow-rose-600/10"
              >
                {submittingCancel && (
                  <Loader2 className="w-4.5 h-4.5 animate-spin mr-1.5" />
                )}
                Xác nhận hủy
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        isOpen={isOpen}
        onClose={close}
        onConfirm={execute}
        title={options?.title || ""}
        description={options?.description || ""}
        confirmText={options?.confirmText}
        cancelText={options?.cancelText}
        variant={options?.variant}
        isLoading={confirming}
      />

      <Footer />
    </div>
  );
}

export default MatchRequestFeedPage;
