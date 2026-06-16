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
  getMyCreatedMatches,
  getMyJoinedRequests,
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

  // Sidebar "KÃ¨o cá»§a tÃ´i" states
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
      toast.error(err.message || "KhÃ´ng thá»ƒ táº£i danh sÃ¡ch kÃ¨o ghÃ©p.");
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
      console.error("Lá»—i khi táº£i dá»¯ liá»‡u cáº¥u hÃ¬nh sÃ¢n vÃ  mÃ´n há»c:", err);
    }
  };

  useEffect(() => {
    fetchFeed();
    fetchDropdowns();
  }, []);

  // Fetch sidebar data when session is ready
  const fetchSidebar = async () => {
    if (!session) return;
    try {
      setLoadingSidebar(true);
      const [created, joined] = await Promise.all([
        getMyCreatedMatches(),
        getMyJoinedRequests(),
      ]);
      setMyCreatedMatches(created);
      setMyJoinedRequests(joined);
    } catch (err: any) {
      console.error("Lá»—i táº£i dá»¯ liá»‡u sidebar:", err);
    } finally {
      setLoadingSidebar(false);
    }
  };

  useEffect(() => {
    if (session) fetchSidebar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session]);

  const getSkillLevelLabel = (level: string) => {
    const config = {
      BEGINNER: "Má»›i báº¯t Ä‘áº§u",
      INTERMEDIATE: "Trung bÃ¬nh",
      ADVANCED: "NÃ¢ng cao",
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
      toast.error("Vui lÃ²ng Ä‘iá»n Ä‘áº§y Ä‘á»§ cÃ¡c thÃ´ng tin báº¯t buá»™c (*)");
      return;
    }

    if (startTime >= endTime) {
      toast.error("Giá» báº¯t Ä‘áº§u pháº£i trÆ°á»›c giá» káº¿t thÃºc!");
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
          ? "ÄÃ£ táº¡o lá»i má»i cÃ¡p kÃ¨o Ä‘á»™i thÃ nh cÃ´ng!"
          : "ÄÃ£ táº¡o lá»i má»i ghÃ©p kÃ¨o thÃ nh cÃ´ng!",
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
      toast.error(err.message || "Lá»—i khi táº¡o kÃ¨o ghÃ©p.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleJoinMatch = async () => {
    if (!selectedRequest) return;
    if (selectedRequest.matchingType === "TEAM_VS_TEAM" && !joinNote.trim()) {
      toast.error("Vui lÃ²ng nháº­p TÃªn Ä‘á»™i bÃ³ng cá»§a báº¡n!");
      return;
    }
    try {
      setSubmittingJoin(true);
      await joinMatchRequest(selectedRequest.matchId, joinNote);
      toast.success(
        "Gá»­i yÃªu cáº§u tham gia kÃ¨o thÃ nh cÃ´ng! Äang chá» chá»§ kÃ¨o phÃª duyá»‡t.",
      );
      setShowJoinDialog(false);
      setJoinNote("");
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lá»—i khi gá»­i yÃªu cáº§u tham gia.");
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
      toast.error(err.message || "KhÃ´ng thá»ƒ táº£i danh sÃ¡ch Ä‘Æ¡n Ä‘Äƒng kÃ½.");
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
      toast.success("ÄÃ£ phÃª duyá»‡t yÃªu cáº§u tham gia thÃ nh cÃ´ng!");
      await fetchJoinRequests(selectedManageMatch.matchId);
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lá»—i khi phÃª duyá»‡t yÃªu cáº§u.");
    } finally {
      setActioningId(null);
    }
  };

  const handleReject = async (joinId: number) => {
    if (!selectedManageMatch) return;
    try {
      setActioningId(joinId);
      await rejectJoinRequest(selectedManageMatch.matchId, joinId);
      toast.success("ÄÃ£ tá»« chá»‘i yÃªu cáº§u tham gia.");
      await fetchJoinRequests(selectedManageMatch.matchId);
      fetchFeed();
    } catch (err: any) {
      toast.error(err.message || "Lá»—i khi tá»« chá»‘i yÃªu cáº§u.");
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

      <div className="container mx-auto px-4 py-8 max-w-7xl">
        {/* Page Header */}
        <div className="flex items-center justify-between mb-8 border-b pb-4">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-800">
              Cá»™ng Ä‘á»“ng GhÃ©p KÃ¨o
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              TÃ¬m Ä‘á»‘i thá»§, ghÃ©p Ä‘á»™i chÆ¡i thá»ƒ thao cÃ¹ng nhá»¯ng ngÆ°á»i cÃ³ chung Ä‘am
              mÃª.
            </p>
          </div>
          <Button
            onClick={() => setShowCreateDialog(true)}
            className="shadow-md shadow-primary/20 gap-2 h-11 px-5 font-bold"
          >
            <Plus className="h-5 w-5" />
            Táº¡o kÃ¨o má»›i
          </Button>
        </div>

        {/* Two-column layout */}
        <div className="flex gap-6 items-start">

          {/* â”€â”€ LEFT COLUMN: Feed â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
          <div className="flex-1 min-w-0">

            {/* Filters */}
            <div className="flex gap-4 mb-8 flex-wrap">
              <div className="relative flex-1 min-w-64">
                <Search className="absolute left-3.5 top-3.5 h-4.5 w-4.5 text-slate-400" />
                <Input
                  placeholder="TÃ¬m kiáº¿m theo tiÃªu Ä‘á», sÃ¢n chÆ¡i..."
                  className="pl-10 h-11 border-slate-200 focus:ring-primary/20 bg-white"
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                />
              </div>

              <Select value={filterSport} onValueChange={setFilterSport}>
                <SelectTrigger className="w-40 h-11 bg-white border-slate-200 font-medium text-slate-700">
                  <SelectValue placeholder="Táº¥t cáº£ mÃ´n" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Táº¥t cáº£ mÃ´n</SelectItem>
                  {sportTypes.map((st) => (
                    <SelectItem key={st.sportTypeId} value={st.sportName}>
                      {st.sportName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={filterLevel} onValueChange={setFilterLevel}>
                <SelectTrigger className="w-40 h-11 bg-white border-slate-200 font-medium text-slate-700">
                  <SelectValue placeholder="Táº¥t cáº£ trÃ¬nh Ä‘á»™" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all-level">Táº¥t cáº£ trÃ¬nh Ä‘á»™</SelectItem>
                  <SelectItem value="BEGINNER">Má»›i báº¯t Ä‘áº§u</SelectItem>
                  <SelectItem value="INTERMEDIATE">Trung bÃ¬nh</SelectItem>
                  <SelectItem value="ADVANCED">NÃ¢ng cao</SelectItem>
                </SelectContent>
              </Select>

              <Select
                value={filterMatchingType}
                onValueChange={setFilterMatchingType}
              >
                <SelectTrigger className="w-40 h-11 bg-white border-slate-200 font-medium text-slate-700">
                  <SelectValue placeholder="HÃ¬nh thá»©c" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Táº¥t cáº£ hÃ¬nh thá»©c</SelectItem>
                  <SelectItem value="INDIVIDUAL">GhÃ©p láº»</SelectItem>
                  <SelectItem value="TEAM_VS_TEAM">CÃ¡p kÃ¨o Äá»™i</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Match Requests Feed */}
            {loadingFeed ? (
              <div className="flex flex-col items-center justify-center py-24 text-slate-400">
                <Loader2 className="w-10 h-10 animate-spin text-primary/50 mb-3" />
                <p className="text-sm font-medium tracking-wide">
                  Äang Ä‘á»“ng bá»™ danh sÃ¡ch kÃ¨o chÆ¡i...
                </p>
              </div>
            ) : filteredRequests.length === 0 ? (
              <div className="text-center py-20 bg-white rounded-xl border border-slate-200 shadow-sm">
                <Users className="w-14 h-14 text-slate-200 mx-auto mb-4" />
                <h3 className="text-lg font-bold text-slate-700 mb-1">
                  ChÆ°a cÃ³ kÃ¨o nÃ o kháº£ dá»¥ng
                </h3>
                <p className="text-slate-400 text-sm max-w-sm mx-auto">
                  KhÃ´ng tÃ¬m tháº¥y kÃ¨o chÆ¡i nÃ o phÃ¹ há»£p vá»›i bá»™ lá»c hoáº·c hiá»‡n táº¡i chÆ°a
                  cÃ³ lá»i má»i nÃ o. Báº¡n hÃ£y lÃ  ngÆ°á»i Ä‘áº§u tiÃªn táº¡o kÃ¨o nhÃ©!
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
                                  â€¢{" "}
                                  {new Date(request.createdAt).toLocaleDateString(
                                    "vi-VN",
                                  )}
                                </span>
                              </div>
                              <h2 className="font-extrabold text-slate-900 text-lg mt-0.5">
                                {request.title}
                              </h2>
                              <div className="flex gap-2 mt-1.5 flex-wrap">
                                {request.matchingType && (
                                  <Badge
                                    variant="outline"
                                    className={
                                      request.matchingType === "TEAM_VS_TEAM"
                                        ? "text-blue-700 border-blue-200 bg-blue-50 hover:bg-blue-50"
                                        : "text-slate-600 border-slate-200 bg-slate-50 hover:bg-slate-50"
                                    }
                                  >
                                    {request.matchingType === "TEAM_VS_TEAM"
                                      ? "GhÃ©p Ä‘á»™i"
                                      : "GhÃ©p láº»"}
                                  </Badge>
                                )}
                                <Badge
                                  variant="outline"
                                  className="text-slate-600 border-slate-200 bg-slate-50 hover:bg-slate-50"
                                >
                                  {request.sportName}
                                </Badge>
                                <Badge
                                  variant="outline"
                                  className={getSkillLevelBadge(request.skillLevel)}
                                >
                                  {getSkillLevelLabel(request.skillLevel)}
                                </Badge>
                                {request.matchStatus === "FULL" && (
                                  <Badge className="bg-amber-500 hover:bg-amber-600 text-white border-0">
                                    ÄÃ£ Äáº§y
                                  </Badge>
                                )}
                              </div>
                            </div>

                            {/* Action Button */}
                            {session?.user?.userId === request.hostUserId ? (
                              <Button
                                size="sm"
                                className="gap-1.5 bg-amber-500 hover:bg-amber-600 text-white shadow-sm font-bold whitespace-nowrap"
                                onClick={() => {
                                  setSelectedManageMatch(request);
                                  setShowManageDialog(true);
                                  getJoinRequests(request.matchId).then(setJoinRequests);
                                }}
                              >
                                <Users className="h-4 w-4" />
                                Quáº£n lÃ½ Ä‘Æ¡n
                              </Button>
                            ) : (
                              <Button
                                size="sm"
                                disabled={
                                  request.matchStatus !== "OPEN" ||
                                  !session
                                }
                                className={`gap-1.5 font-bold whitespace-nowrap ${
                                  request.matchStatus !== "OPEN"
                                    ? "bg-slate-300 text-slate-500 cursor-not-allowed"
                                    : "bg-primary hover:bg-primary/90 text-white"
                                }`}
                                onClick={() => {
                                  setSelectedRequest(request);
                                  setShowJoinDialog(true);
                                }}
                              >
                                {request.currentPlayers >=
                                (request.matchingType === "TEAM_VS_TEAM"
                                  ? 2
                                  : request.maxPlayers)
                                  ? request.matchingType === "TEAM_VS_TEAM"
                                    ? "ÄÃ£ Äá»§ Äá»™i"
                                    : "ÄÃ£ Äá»§ NgÆ°á»i"
                                  : request.matchingType === "TEAM_VS_TEAM"
                                    ? "CÃ¡p kÃ¨o"
                                    : "Tham gia kÃ¨o"}
                              </Button>
                            )}
                          </div>

                          {/* Description */}
                          <p className="mb-4 text-slate-600 text-sm leading-relaxed">
                            {request.description || "ChÆ°a cÃ³ mÃ´ táº£ chi tiáº¿t."}
                          </p>

                          {/* Details */}
                          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-xs font-semibold text-slate-500 border-t pt-4">
                            <div className="flex items-center">
                              <Calendar className="h-4 w-4 mr-2.5 text-primary" />
                              <span>
                                NgÃ y Ä‘Ã¡:{" "}
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
                                Giá» chÆ¡i:{" "}
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
                                Äá»‹a Ä‘iá»ƒm:{" "}
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
                                    Tráº¡ng thÃ¡i:{" "}
                                    <strong className="text-slate-700">
                                      {request.currentPlayers}/2 Ä‘á»™i
                                    </strong>
                                  </>
                                ) : (
                                  <>
                                    SÄ© sá»‘:{" "}
                                    <strong className="text-slate-700">
                                      {request.currentPlayers}/{request.maxPlayers}{" "}
                                      thÃ nh viÃªn
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
                                      Chia Ä‘Ã´i tiá»n sÃ¢n:{" "}
                                      <strong className="text-emerald-700">
                                        {request.pricePerPlayer.toLocaleString(
                                          "vi-VN",
                                        )}{" "}
                                        Ä‘ / Ä‘á»™i
                                      </strong>
                                    </>
                                  ) : (
                                    <>
                                      Chia tiá»n sÃ¢n:{" "}
                                      <strong className="text-emerald-700">
                                        {request.pricePerPlayer.toLocaleString(
                                          "vi-VN",
                                        )}{" "}
                                        Ä‘ / ngÆ°á»i
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
          {/* â”€â”€ END LEFT COLUMN â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}

          {/* â”€â”€ RIGHT SIDEBAR: KÃ¨o cá»§a tÃ´i â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
          {session && (
            <aside className="w-80 shrink-0 space-y-5 sticky top-24 self-start">
              {/* Panel: KÃ¨o tÃ´i táº¡o */}
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-4 py-3 bg-gradient-to-r from-primary/5 to-primary/10 border-b border-slate-100 flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-primary" />
                  <h3 className="text-sm font-bold text-slate-800">KÃ¨o tÃ´i táº¡o</h3>
                  <span className="ml-auto text-xs font-bold bg-primary/10 text-primary px-2 py-0.5 rounded-full">
                    {myCreatedMatches.length}
                  </span>
                </div>
                <div className="divide-y divide-slate-100 max-h-80 overflow-y-auto">
                  {loadingSidebar ? (
                    <div className="flex justify-center py-6">
                      <Loader2 className="w-5 h-5 animate-spin text-primary/50" />
                    </div>
                  ) : myCreatedMatches.length === 0 ? (
                    <div className="text-center py-8 px-4">
                      <p className="text-xs text-slate-400">Báº¡n chÆ°a táº¡o kÃ¨o nÃ o.</p>
                    </div>
                  ) : (
                    myCreatedMatches.map((m) => (
                      <div
                        key={m.matchId}
                        className="px-4 py-3 hover:bg-slate-50 transition-colors cursor-pointer"
                        onClick={() => {
                          setSelectedManageMatch(m);
                          setShowManageDialog(true);
                          getJoinRequests(m.matchId).then(setJoinRequests);
                        }}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-bold text-slate-800 truncate">{m.title}</p>
                            <p className="text-xs text-slate-500 mt-0.5">
                              {m.sportName} â€¢ {new Date(m.playDate).toLocaleDateString("vi-VN")}
                            </p>
                            <p className="text-xs text-slate-400">
                              {m.startTime.substring(0, 5)} â€“ {m.endTime.substring(0, 5)} Â· {m.stadiumName}
                            </p>
                          </div>
                          <Badge
                            className={`text-[10px] shrink-0 ${
                              m.matchStatus === "OPEN"
                                ? "bg-emerald-500 hover:bg-emerald-600"
                                : m.matchStatus === "FULL"
                                ? "bg-amber-500 hover:bg-amber-600"
                                : "bg-slate-400 hover:bg-slate-500"
                            }`}
                          >
                            {m.matchStatus === "OPEN" ? "Äang má»Ÿ" : m.matchStatus === "FULL" ? "Äáº§y" : m.matchStatus}
                          </Badge>
                        </div>
                        <div className="mt-2">
                          <div className="w-full bg-slate-100 rounded-full h-1.5">
                            <div
                              className="bg-primary h-1.5 rounded-full"
                              style={{
                                width: `${Math.min(100, (m.currentPlayers / (m.matchingType === "TEAM_VS_TEAM" ? 2 : m.maxPlayers)) * 100)}%`,
                              }}
                            />
                          </div>
                          <p className="text-[10px] text-slate-400 mt-0.5 text-right">
                            {m.currentPlayers}/{m.matchingType === "TEAM_VS_TEAM" ? "2 Ä‘á»™i" : `${m.maxPlayers} ngÆ°á»i`}
                          </p>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Panel: ÄÆ¡n tÃ´i Ä‘Ã£ gá»­i */}
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-4 py-3 bg-gradient-to-r from-blue-50 to-blue-100/50 border-b border-slate-100 flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 text-blue-600" />
                  <h3 className="text-sm font-bold text-slate-800">ÄÆ¡n tÃ´i Ä‘Ã£ gá»­i</h3>
                  <span className="ml-auto text-xs font-bold bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">
                    {myJoinedRequests.filter((r) => r.requestStatus === "PENDING").length} chá»
                  </span>
                </div>
                <div className="divide-y divide-slate-100 max-h-80 overflow-y-auto">
                  {loadingSidebar ? (
                    <div className="flex justify-center py-6">
                      <Loader2 className="w-5 h-5 animate-spin text-blue-400/60" />
                    </div>
                  ) : myJoinedRequests.length === 0 ? (
                    <div className="text-center py-8 px-4">
                      <p className="text-xs text-slate-400">Báº¡n chÆ°a gá»­i Ä‘Æ¡n tham gia nÃ o.</p>
                    </div>
                  ) : (
                    myJoinedRequests.map((req) => (
                      <div key={req.joinId} className="px-4 py-3 hover:bg-slate-50 transition-colors">
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 min-w-0">
                            <p className="text-xs text-slate-400 mb-0.5">KÃ¨o #{req.matchId}</p>
                            {req.message && (
                              <p className="text-xs text-slate-600 italic truncate">"{req.message}"</p>
                            )}
                            <p className="text-[10px] text-slate-400 mt-0.5">
                              {new Date(req.createdAt).toLocaleDateString("vi-VN")}
                            </p>
                          </div>
                          <Badge
                            className={`text-[10px] shrink-0 ${
                              req.requestStatus === "PENDING"
                                ? "bg-amber-400 hover:bg-amber-500"
                                : req.requestStatus === "APPROVED"
                                ? "bg-emerald-500 hover:bg-emerald-600"
                                : "bg-slate-400 hover:bg-slate-500"
                            }`}
                          >
                            {req.requestStatus === "PENDING"
                              ? "Chá» duyá»‡t"
                              : req.requestStatus === "APPROVED"
                              ? "ÄÃ£ duyá»‡t"
                              : "Tá»« chá»‘i"}
                          </Badge>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </aside>
          )}
          {/* â”€â”€ END RIGHT SIDEBAR â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}

        </div>
      </div>


      {/* Create Match Request Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent className="max-w-2xl bg-white border border-slate-200 p-6">
          <DialogHeader className="border-b pb-3 mb-4">
            <DialogTitle className="text-xl font-bold text-slate-800">
              Táº¡o lá»i má»i tÃ¬m Ä‘á»‘i thá»§
            </DialogTitle>
          </DialogHeader>

          <form onSubmit={handleCreateMatch} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="title" className="font-bold text-slate-700">
                TiÃªu Ä‘á» kÃ¨o chÆ¡i *
              </Label>
              <Input
                id="title"
                placeholder="VD: Cáº§n tÃ¬m 3 báº¡n giao lÆ°u bÃ³ng Ä‘Ã¡ sÃ¢n Hoa LÆ°"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="border-slate-200"
              />
            </div>

            <div className="space-y-2">
              <Label className="font-bold text-slate-700">
                HÃ¬nh thá»©c ghÃ©p kÃ¨o *
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
                  <span className="font-bold text-sm">GhÃ©p ngÆ°á»i chÆ¡i láº»</span>
                  <span className="text-[10px] opacity-80 mt-0.5">
                    TÃ¬m thÃªm thÃ nh viÃªn cho Ä‘á»§ Ä‘á»™i
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
                  <span className="font-bold text-sm">CÃ¡p kÃ¨o Äá»™i vs Äá»™i</span>
                  <span className="text-[10px] opacity-80 mt-0.5">
                    Äá»™i cá»§a báº¡n tÃ¬m Ä‘á»™i Ä‘á»‘i thá»§ thÃ¡ch Ä‘áº¥u
                  </span>
                </button>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="sport" className="font-bold text-slate-700">
                  MÃ´n thá»ƒ thao *
                </Label>
                <Select value={sportTypeId} onValueChange={setSportTypeId}>
                  <SelectTrigger id="sport" className="border-slate-200">
                    <SelectValue placeholder="Chá»n mÃ´n thá»ƒ thao" />
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
                  TrÃ¬nh Ä‘á»™ yÃªu cáº§u *
                </Label>
                <Select
                  value={skillLevel}
                  onValueChange={(val: any) => setSkillLevel(val)}
                >
                  <SelectTrigger id="skill" className="border-slate-200">
                    <SelectValue placeholder="Chá»n trÃ¬nh Ä‘á»™" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="BEGINNER">Má»›i báº¯t Ä‘áº§u</SelectItem>
                    <SelectItem value="INTERMEDIATE">Trung bÃ¬nh</SelectItem>
                    <SelectItem value="ADVANCED">NÃ¢ng cao</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date" className="font-bold text-slate-700">
                  NgÃ y chÆ¡i *
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
                  Giá» báº¯t Ä‘áº§u *
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
                  Giá» káº¿t thÃºc *
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
                  SÃ¢n chÆ¡i *
                </Label>
                <Select value={stadiumId} onValueChange={setStadiumId}>
                  <SelectTrigger
                    id="venue-select"
                    className="border-slate-200 w-full overflow-hidden [&>span]:truncate"
                  >
                    <SelectValue placeholder="Chá»n sÃ¢n bÃ³ng" />
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
                  Sá»‘ ngÆ°á»i chÆ¡i tá»‘i Ä‘a *
                </Label>
                {matchingType === "TEAM_VS_TEAM" ? (
                  <div className="h-10 border border-slate-200 bg-slate-50 rounded-md flex items-center px-3 text-xs font-semibold text-slate-500">
                    Máº·c Ä‘á»‹nh lÃ  2 Ä‘á»™i (Chá»§ nhÃ  & Äá»‘i thá»§)
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
                      ? "Chia Ä‘Ã´i tiá»n sÃ¢n (50/50)"
                      : "Chia tiá»n sÃ¢n (Split Price)"}
                  </Label>
                  <span className="text-[11px] text-slate-500 font-medium">
                    {matchingType === "TEAM_VS_TEAM"
                      ? "Báº­t náº¿u báº¡n muá»‘n Ä‘á»™i Ä‘á»‘i thá»§ cÃ¹ng chia sáº» má»™t ná»­a chi phÃ­ thuÃª sÃ¢n."
                      : "Báº­t náº¿u báº¡n muá»‘n nhá»¯ng ngÆ°á»i tham gia cÃ¹ng chia sáº» chi phÃ­ sÃ¢n chÆ¡i."}
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
                      ? "Chi phÃ­ má»—i Ä‘á»™i (Ä‘) *"
                      : "Chi phÃ­ má»—i ngÆ°á»i chÆ¡i (Ä‘) *"}
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
                MÃ´ táº£ thÃªm
              </Label>
              <Textarea
                id="desc"
                placeholder="VD: NhÃ³m thÃ¢n thiá»‡n, Ä‘Ã¡ giao lÆ°u há»c há»i, máº·c Ã¡o mÃ u Ä‘á» nhÃ© cáº£ nhÃ ..."
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
                Há»§y
              </Button>
              <Button
                type="submit"
                className="flex-1 font-bold shadow-md shadow-primary/10"
                disabled={submitting}
              >
                {submitting && (
                  <Loader2 className="w-4.5 h-4.5 animate-spin mr-1.5" />
                )}
                Táº¡o lá»i má»i
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
              Xin tham gia kÃ¨o chÆ¡i
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
                      MÃ´n thá»ƒ thao:{" "}
                      <strong className="text-slate-700">
                        {selectedRequest.sportName}
                      </strong>
                    </div>
                    <div>
                      Thá»i gian:{" "}
                      <strong className="text-slate-700">
                        {new Date(selectedRequest.playDate).toLocaleDateString(
                          "vi-VN",
                        )}{" "}
                        ({selectedRequest.startTime.substring(0, 5)} -{" "}
                        {selectedRequest.endTime.substring(0, 5)})
                      </strong>
                    </div>
                    <div>
                      SÃ¢n váº­n Ä‘á»™ng:{" "}
                      <strong className="text-slate-700">
                        {selectedRequest.stadiumName}
                      </strong>
                    </div>
                    <div>
                      {selectedRequest.matchingType === "TEAM_VS_TEAM" ? (
                        <>
                          Tráº¡ng thÃ¡i:{" "}
                          <strong className="text-slate-700">
                            {selectedRequest.currentPlayers}/2 Ä‘á»™i
                          </strong>
                        </>
                      ) : (
                        <>
                          SÄ© sá»‘:{" "}
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
                    ? "TÃªn Ä‘á»™i bÃ³ng cá»§a báº¡n & lá»i nháº¯n *"
                    : "Lá»i nháº¯n tá»›i chá»§ kÃ¨o (khÃ´ng báº¯t buá»™c)"}
                </Label>
                <Textarea
                  id="join-note"
                  placeholder={
                    selectedRequest.matchingType === "TEAM_VS_TEAM"
                      ? "VD: FC Phá»§i Quáº­n 1, trÃ¬nh Ä‘á»™ trung bÃ¬nh khÃ¡, xin cÃ¡p kÃ¨o Ä‘Ã¡ giao lÆ°u..."
                      : "VD: MÃ¬nh Ä‘Ã¡ á»Ÿ vá»‹ trÃ­ tiá»n Ä‘áº¡o, trÃ¬nh Ä‘á»™ trung bÃ¬nh, cho mÃ¬nh tham gia vá»›i nhÃ©..."
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
                  Há»§y
                </Button>
                <Button
                  onClick={handleJoinMatch}
                  className="flex-1 font-bold shadow-md shadow-primary/10"
                  disabled={submittingJoin}
                >
                  {submittingJoin && (
                    <Loader2 className="w-4.5 h-4.5 animate-spin mr-1.5" />
                  )}
                  Gá»­i yÃªu cáº§u tham gia
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Host Manage Join Requests Dialog */}
      <Dialog open={showManageDialog} onOpenChange={setShowManageDialog}>
        <DialogContent className="w-full max-w-2xl bg-white border border-slate-200 p-6 max-h-[85vh] overflow-y-auto overflow-x-hidden">
          <DialogHeader className="border-b pb-3 mb-4">
            <DialogTitle className="text-xl font-bold text-slate-800">
              Quáº£n lÃ½ Ä‘Æ¡n Ä‘Äƒng kÃ½ tham gia
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
                    {selectedManageMatch.matchStatus === "OPEN" ? "Äang nháº­n Ä‘Æ¡n" : "ÄÃ£ ÄÃ³ng / Äáº§y"}
                  </Badge>
                </div>
                <div className="grid grid-cols-2 gap-y-2 gap-x-4 text-xs font-semibold text-slate-500 mt-3">
                  <div>SÃ¢n: <span className="text-slate-700">{selectedManageMatch.stadiumName}</span></div>
                  <div>NgÃ y: <span className="text-slate-700">{new Date(selectedManageMatch.playDate).toLocaleDateString("vi-VN")}</span></div>
                  <div>Thá»i gian: <span className="text-slate-700">{selectedManageMatch.startTime.substring(0, 5)} - {selectedManageMatch.endTime.substring(0, 5)}</span></div>
                  <div>SÄ© sá»‘ hiá»‡n táº¡i: <span className="text-slate-700">{selectedManageMatch.currentPlayers}/{selectedManageMatch.matchingType === "TEAM_VS_TEAM" ? "2 Ä‘á»™i" : selectedManageMatch.maxPlayers}</span></div>
                </div>
              </div>

              {/* Join Requests List */}
              <div className="space-y-4">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Danh sÃ¡ch Ä‘Æ¡n Ä‘Äƒng kÃ½
                </h4>

                {loadingRequests ? (
                  <div className="flex justify-center items-center py-10">
                    <Loader2 className="w-8 h-8 animate-spin text-primary/50" />
                  </div>
                ) : joinRequests.length === 0 ? (
                  <div className="text-center py-12 border border-dashed border-slate-200 rounded-xl bg-slate-50/50">
                    <Users className="w-10 h-10 text-slate-300 mx-auto mb-2.5" />
                    <p className="text-sm font-semibold text-slate-500">ChÆ°a cÃ³ ai Ä‘Äƒng kÃ½ tham gia kÃ¨o nÃ y</p>
                    <p className="text-xs text-slate-400 mt-1">YÃªu cáº§u tham gia tá»« ngÆ°á»i dÃ¹ng khÃ¡c sáº½ hiá»ƒn thá»‹ táº¡i Ä‘Ã¢y.</p>
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
                              {req.email} â€¢ {new Date(req.createdAt).toLocaleDateString("vi-VN")}
                            </div>
                            {req.message && (
                              <div className="mt-2 bg-slate-50 border border-slate-100 p-2 rounded-lg text-xs text-slate-600 font-medium">
                                <span className="font-bold text-slate-700">
                                  {selectedManageMatch.matchingType === "TEAM_VS_TEAM" ? "TÃªn Ä‘á»™i bÃ³ng: " : "Lá»i nháº¯n: "}
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
                                Tá»« chá»‘i
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
                                PhÃª duyá»‡t
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
                                ? "ÄÃ£ PhÃª Duyá»‡t"
                                : req.requestStatus === "REJECTED"
                                  ? "ÄÃ£ Tá»« Chá»‘i"
                                  : "ÄÃ£ Há»§y"}
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
