'use client'

import { useState, useEffect, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  AlertCircle,
  MessageSquare,
  Users,
  Building,
  MapPin,
  Home,
  UserCog,
  Settings,
  Send,
  CheckCircle2,
  Clock,
  ShieldAlert
} from "lucide-react";
import Link from "next/link";
import { get, post } from "@/lib/api";
import { toast } from "sonner";

type ResponseItem = {
  from: string;
  message: string;
  time: string;
};

type Complaint = {
  complaintId: number;
  subject: string;
  content: string;
  status: string;
  priority: string;
  createdAt: string;
  resolvedAt?: string;
  resolution?: string;
  bookingId: number;
  bookingStatus: string;
  stadiumId: number;
  stadiumName: string;
  ownerName: string;
  ownerEmail: string;
  customerName: string;
  customerEmail: string;
  responses: ResponseItem[];
};

const DEFAULT_COMPLAINTS: Complaint[] = [
  {
    complaintId: 1,
    subject: "Sân thực tế xuống cấp nghiêm trọng",
    content: "Mặt cỏ bị rách nhiều chỗ, hệ thống đèn chiếu sáng ban đêm bị hỏng mất một nửa khiến chúng tôi không thể đá bóng an toàn.",
    status: "open",
    priority: "high",
    createdAt: "2026-06-20 18:30",
    bookingId: 12,
    bookingStatus: "COMPLETED",
    stadiumId: 101,
    stadiumName: "Sân bóng cỏ nhân tạo Phú Nhuận",
    ownerName: "Nguyễn Văn A",
    ownerEmail: "owner1@sportvenue.com",
    customerName: "Lê Văn C",
    customerEmail: "customer1@sportvenue.com",
    responses: []
  },
  {
    complaintId: 2,
    subject: "Thái độ nhân viên giữ xe thiếu tôn trọng",
    content: "Nhân viên sân Arena quát mắng khách hàng và tính sai tiền gửi xe so với quy định niêm yết.",
    status: "in_progress",
    priority: "medium",
    createdAt: "2026-06-19 14:15",
    bookingId: 15,
    bookingStatus: "COMPLETED",
    stadiumId: 105,
    stadiumName: "Tổ hợp Thể thao Arena",
    ownerName: "Trần Thị B",
    ownerEmail: "owner2@sportvenue.com",
    customerName: "Phạm Thị D",
    customerEmail: "customer2@sportvenue.com",
    responses: [
      {
        from: "Chủ sân",
        message: "Chào bạn, chúng tôi chân thành xin lỗi vì trải nghiệm không tốt này. Chúng tôi sẽ nhắc nhở bộ phận bảo vệ ngay lập tức.",
        time: "2026-06-19 15:30"
      }
    ]
  }
];

function AdminComplaintsPage() {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [replyMessage, setReplyMessage] = useState("");
  const [resolutionText, setResolutionText] = useState("");
  const [showResolveDialog, setShowResolveDialog] = useState(false);
  const [filterStatus, setFilterStatus] = useState("all");
  const [filterPriority, setFilterPriority] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");

  const fetchComplaints = useCallback(async () => {
    try {
      const data = await get<Complaint[]>("/admin/complaints");
      if (data && Array.isArray(data)) {
        setComplaints(data);
        localStorage.setItem("admin_complaints_data", JSON.stringify(data));
        setSelectedComplaint(prev => {
          if (prev) {
            return data.find(c => c.complaintId === prev.complaintId) || null;
          }
          return null;
        });
      } else {
        throw new Error("Dữ liệu khiếu nại không hợp lệ");
      }
    } catch (error) {
      console.warn("Backend offline or error, loading local fallback data:", error);
      const stored = localStorage.getItem("admin_complaints_data");
      if (stored) {
        setComplaints(JSON.parse(stored));
      } else {
        localStorage.setItem("admin_complaints_data", JSON.stringify(DEFAULT_COMPLAINTS));
        setComplaints(DEFAULT_COMPLAINTS);
      }
    }
  }, []);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  const saveComplaintsLocal = (updatedList: Complaint[]) => {
    setComplaints(updatedList);
    localStorage.setItem("admin_complaints_data", JSON.stringify(updatedList));
    setSelectedComplaint(prev => {
      if (prev) {
        return updatedList.find(c => c.complaintId === prev.complaintId) || null;
      }
      return null;
    });
  };

  const handleSendMessage = async () => {
    if (!replyMessage.trim() || !selectedComplaint) return;

    try {
      await post<unknown>(`/admin/complaints/${selectedComplaint.complaintId}/reply`, {
        message: replyMessage.trim()
      });
      setReplyMessage("");
      toast.success("Gửi phản hồi của Admin thành công!");
      fetchComplaints();
    } catch (error) {
      console.warn("Backend API reply failed, fall back to local update:", error);
      const nowStr = new Date().toISOString().replace('T', ' ').substring(0, 16);
      const newResponse: ResponseItem = {
        from: "Admin",
        message: replyMessage.trim(),
        time: nowStr
      };

      const updatedList = complaints.map(c => {
        if (c.complaintId === selectedComplaint.complaintId) {
          const nextResponses = [...c.responses, newResponse];
          const nextStatus = c.status === "open" ? "in_progress" : c.status;
          return { ...c, responses: nextResponses, status: nextStatus };
        }
        return c;
      });

      setReplyMessage("");
      toast.success("Gửi phản hồi thành công (Local Mode)!");
      saveComplaintsLocal(updatedList);
    }
  };

  const handleResolveComplaint = async () => {
    if (!resolutionText.trim() || !selectedComplaint) return;

    try {
      await post<unknown>(`/admin/complaints/${selectedComplaint.complaintId}/resolve`, {
        resolution: resolutionText.trim()
      });
      setResolutionText("");
      setShowResolveDialog(false);
      toast.success("Giải quyết khiếu nại thành công!");
      fetchComplaints();
    } catch (error) {
      console.warn("Backend API resolve failed, fall back to local update:", error);
      const nowStr = new Date().toISOString().replace('T', ' ').substring(0, 16);
      const newResponse: ResponseItem = {
        from: "Admin",
        message: `Đã giải quyết: ${resolutionText.trim()}`,
        time: nowStr
      };

      const updatedList = complaints.map(c => {
        if (c.complaintId === selectedComplaint.complaintId) {
          return {
            ...c,
            status: "resolved",
            resolvedAt: nowStr,
            resolution: resolutionText.trim(),
            responses: [...c.responses, newResponse]
          };
        }
        return c;
      });

      setResolutionText("");
      setShowResolveDialog(false);
      toast.success("Đã ghi nhận đóng khiếu nại (Local Mode)!");
      saveComplaintsLocal(updatedList);
    }
  };

  const getPriorityConfig = (priority?: string) => {
    switch (priority?.toLowerCase()) {
      case "high":
        return { label: "Cao", color: "bg-red-500/10 text-red-500 border-red-500/20" };
      case "low":
        return { label: "Thấp", color: "bg-emerald-500/10 text-emerald-500 border-emerald-500/20" };
      default:
        return { label: "Trung bình", color: "bg-amber-500/10 text-amber-500 border-amber-500/20" };
    }
  };

  const getStatusConfig = (status: string) => {
    switch (status.toLowerCase()) {
      case "resolved":
        return { label: "Đã giải quyết", color: "bg-emerald-500 text-white" };
      case "in_progress":
        return { label: "Đang xử lý", color: "bg-blue-500 text-white" };
      default:
        return { label: "Mới nhận", color: "bg-amber-500 text-white" };
    }
  };

  const totalCount = complaints.length;
  const openCount = complaints.filter(c => c.status === "open").length;
  const progressCount = complaints.filter(c => c.status === "in_progress").length;
  const resolvedCount = complaints.filter(c => c.status === "resolved").length;

  const filteredComplaints = complaints.filter(c => {
    const matchesStatus = filterStatus === "all" || c.status.toLowerCase() === filterStatus.toLowerCase();
    const matchesPriority = filterPriority === "all" || c.priority.toLowerCase() === filterPriority.toLowerCase();
    const matchesSearch = c.subject.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.content.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.stadiumName.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesStatus && matchesPriority && matchesSearch;
  });

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white">
      <Header />

      <div className="flex flex-1">
        {/* Sidebar */}
        <aside className="w-64 min-h-screen bg-neutral-900 border-r border-neutral-800 p-5 flex flex-col justify-between">
          <div>
            <h2 className="mb-6 px-3 text-xs font-bold tracking-widest text-neutral-400 uppercase">Quản trị hệ thống</h2>
            <nav className="space-y-1.5">
              <Link href="/admin/dashboard" className="block w-full">
                <Button variant="ghost" className="w-full justify-start text-neutral-400 hover:text-white hover:bg-neutral-800" size="sm">
                  <Home className="mr-3 h-4 w-4" />
                  Dashboard
                </Button>
              </Link>
              <Link href="/admin/users" className="block w-full">
                <Button variant="ghost" className="w-full justify-start text-neutral-400 hover:text-white hover:bg-neutral-800" size="sm">
                  <Users className="mr-3 h-4 w-4" />
                  Người dùng
                </Button>
              </Link>
              <Link href="/admin/owner-approvals" className="block w-full">
                <Button variant="ghost" className="w-full justify-start text-neutral-400 hover:text-white hover:bg-neutral-800" size="sm">
                  <Building className="mr-3 h-4 w-4" />
                  Chủ sân
                </Button>
              </Link>
              <Link href="/admin/stadium-approvals" className="block w-full">
                <Button variant="ghost" className="w-full justify-start text-neutral-400 hover:text-white hover:bg-neutral-800" size="sm">
                  <MapPin className="mr-3 h-4 w-4" />
                  Sân bóng
                </Button>
              </Link>
              <Link href="/admin/sport-categories" className="block w-full">
                <Button variant="ghost" className="w-full justify-start text-neutral-400 hover:text-white hover:bg-neutral-800" size="sm">
                  <UserCog className="mr-3 h-4 w-4" />
                  Danh mục
                </Button>
              </Link>
              <Button variant="default" className="w-full justify-start bg-indigo-600 hover:bg-indigo-700 text-white" size="sm">
                <ShieldAlert className="mr-3 h-4 w-4" />
                Khiếu nại
              </Button>
              <Button variant="ghost" className="w-full justify-start text-neutral-400 hover:text-white hover:bg-neutral-800" size="sm">
                <Settings className="mr-3 h-4 w-4" />
                Cài đặt
              </Button>
            </nav>
          </div>
          <div className="p-3 bg-neutral-950/60 rounded-xl border border-neutral-800/80">
            <p className="text-xs text-neutral-400">Đang hoạt động</p>
            <p className="text-sm font-semibold text-indigo-400">Quản trị viên Hệ thống</p>
          </div>
        </aside>

        {/* Main Content Area */}
        <main className="flex-1 p-8 flex flex-col gap-6 bg-neutral-950">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight text-white">Quản lý khiếu nại hệ thống</h1>
              <p className="text-sm text-neutral-400 mt-1">Giám sát và hòa giải khiếu nại giữa khách hàng và các chủ sân thể thao.</p>
            </div>
          </div>

          {/* Quick Metrics */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Card className="bg-neutral-900 border-neutral-800 shadow-xl">
              <CardContent className="p-5">
                <div className="text-xs text-neutral-400 font-medium">Tổng khiếu nại</div>
                <div className="text-2xl font-bold text-white mt-1">{totalCount}</div>
              </CardContent>
            </Card>
            <Card className="bg-neutral-900 border-neutral-800 shadow-xl">
              <CardContent className="p-5">
                <div className="text-xs text-neutral-400 font-medium">Chưa phản hồi (Mới)</div>
                <div className="text-2xl font-bold text-amber-500 mt-1">{openCount}</div>
              </CardContent>
            </Card>
            <Card className="bg-neutral-900 border-neutral-800 shadow-xl">
              <CardContent className="p-5">
                <div className="text-xs text-neutral-400 font-medium">Đang giải quyết</div>
                <div className="text-2xl font-bold text-blue-400 mt-1">{progressCount}</div>
              </CardContent>
            </Card>
            <Card className="bg-neutral-900 border-neutral-800 shadow-xl">
              <CardContent className="p-5">
                <div className="text-xs text-neutral-400 font-medium">Đã đóng</div>
                <div className="text-2xl font-bold text-emerald-500 mt-1">{resolvedCount}</div>
              </CardContent>
            </Card>
          </div>

          {/* Filters Bar */}
          <div className="flex flex-col sm:flex-row gap-3 bg-neutral-900 p-4 rounded-xl border border-neutral-800 shadow-md">
            <div className="flex-1">
              <Input
                placeholder="Tìm theo chủ đề, nội dung, tên khách hàng, tên sân..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="bg-neutral-950 border-neutral-800 text-sm text-neutral-200 focus-visible:ring-indigo-600 focus-visible:ring-1"
              />
            </div>
            <div className="flex gap-3">
              <Select value={filterStatus} onValueChange={setFilterStatus}>
                <SelectTrigger className="w-40 bg-neutral-950 border-neutral-800 text-neutral-200">
                  <SelectValue placeholder="Trạng thái" />
                </SelectTrigger>
                <SelectContent className="bg-neutral-900 border-neutral-800 text-neutral-200">
                  <SelectItem value="all">Mọi trạng thái</SelectItem>
                  <SelectItem value="open">Mới nhận</SelectItem>
                  <SelectItem value="in_progress">Đang xử lý</SelectItem>
                  <SelectItem value="resolved">Đã giải quyết</SelectItem>
                </SelectContent>
              </Select>

              <Select value={filterPriority} onValueChange={setFilterPriority}>
                <SelectTrigger className="w-40 bg-neutral-950 border-neutral-800 text-neutral-200">
                  <SelectValue placeholder="Độ ưu tiên" />
                </SelectTrigger>
                <SelectContent className="bg-neutral-900 border-neutral-800 text-neutral-200">
                  <SelectItem value="all">Mọi ưu tiên</SelectItem>
                  <SelectItem value="high">Ưu tiên Cao</SelectItem>
                  <SelectItem value="medium">Trung bình</SelectItem>
                  <SelectItem value="low">Thấp</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Split View */}
          <div className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6 min-h-[550px]">
            {/* List side */}
            <div className="lg:col-span-5 flex flex-col gap-3 max-h-[700px] overflow-y-auto pr-1">
              {filteredComplaints.length === 0 ? (
                <div className="flex-1 flex flex-col items-center justify-center border border-dashed border-neutral-800 rounded-xl p-8 text-neutral-500 bg-neutral-900/20">
                  <AlertCircle className="h-10 w-10 text-neutral-600 mb-2" />
                  <p className="text-sm">Không tìm thấy khiếu nại nào phù hợp</p>
                </div>
              ) : (
                filteredComplaints.map(c => {
                  const priority = getPriorityConfig(c.priority);
                  const status = getStatusConfig(c.status);
                  const isSelected = selectedComplaint?.complaintId === c.complaintId;
                  return (
                    <Card
                      key={c.complaintId}
                      onClick={() => setSelectedComplaint(c)}
                      className={`cursor-pointer transition-all border shadow-sm ${
                        isSelected
                          ? "bg-indigo-950/20 border-indigo-500/80 shadow-indigo-950/50"
                          : "bg-neutral-900/90 border-neutral-800 hover:border-neutral-700"
                      }`}
                    >
                      <CardContent className="p-4 flex flex-col gap-3">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-mono text-neutral-500">ID: #{c.complaintId}</span>
                          <div className="flex gap-1.5">
                            <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full border ${priority.color}`}>
                              {priority.label}
                            </span>
                            <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${status.color}`}>
                              {status.label}
                            </span>
                          </div>
                        </div>

                        <div>
                          <h3 className="font-semibold text-sm text-neutral-200 line-clamp-1">{c.subject}</h3>
                          <p className="text-xs text-neutral-400 mt-1 line-clamp-2">{c.content}</p>
                        </div>

                        <div className="flex items-center justify-between border-t border-neutral-800/60 pt-3 text-[11px] text-neutral-500">
                          <div>
                            Khách: <span className="text-neutral-300 font-medium">{c.customerName}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Clock className="h-3 w-3" />
                            {c.createdAt}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  );
                })
              )}
            </div>

            {/* Chat/Detail side */}
            <div className="lg:col-span-7 flex flex-col bg-neutral-900 border border-neutral-800 rounded-xl overflow-hidden shadow-xl max-h-[700px]">
              {selectedComplaint ? (
                <div className="flex-1 flex flex-col h-full overflow-hidden">
                  {/* Detailed Card Header */}
                  <div className="p-5 border-b border-neutral-800 bg-neutral-900/80 backdrop-blur-sm flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
                    <div>
                      <div className="flex items-center gap-2 mb-1.5">
                        <span className="text-xs font-mono text-neutral-500">Khiếu nại #{selectedComplaint.complaintId}</span>
                        <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full border ${getPriorityConfig(selectedComplaint.priority).color}`}>
                          {getPriorityConfig(selectedComplaint.priority).label}
                        </span>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${getStatusConfig(selectedComplaint.status).color}`}>
                          {getStatusConfig(selectedComplaint.status).label}
                        </span>
                      </div>
                      <h2 className="text-base font-semibold text-white">{selectedComplaint.subject}</h2>
                    </div>

                    {selectedComplaint.status !== "resolved" && (
                      <Button
                        size="sm"
                        onClick={() => setShowResolveDialog(true)}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white font-medium flex items-center gap-1.5"
                      >
                        <CheckCircle2 className="h-4 w-4" />
                        Đóng khiếu nại
                      </Button>
                    )}
                  </div>

                  {/* Complaint Details Panel */}
                  <div className="px-5 py-4 bg-neutral-950/40 border-b border-neutral-800/80 text-xs grid grid-cols-2 md:grid-cols-4 gap-4 text-neutral-300">
                    <div>
                      <p className="text-neutral-500 font-medium">Người gửi khiếu nại</p>
                      <p className="font-semibold text-neutral-200 mt-0.5">{selectedComplaint.customerName}</p>
                      <p className="text-[10px] text-neutral-500 font-mono">{selectedComplaint.customerEmail}</p>
                    </div>
                    <div>
                      <p className="text-neutral-500 font-medium">Đối tượng bị khiếu nại</p>
                      <p className="font-semibold text-neutral-200 mt-0.5">{selectedComplaint.stadiumName}</p>
                      <p className="text-[10px] text-neutral-500 font-mono">Chủ: {selectedComplaint.ownerName}</p>
                    </div>
                    <div>
                      <p className="text-neutral-500 font-medium">Đơn đặt sân liên quan</p>
                      <p className="font-semibold text-neutral-200 mt-0.5">Booking #{selectedComplaint.bookingId}</p>
                      <p className="text-[10px] text-indigo-400 font-bold">{selectedComplaint.bookingStatus}</p>
                    </div>
                    <div>
                      <p className="text-neutral-500 font-medium">Thời gian tạo</p>
                      <p className="font-semibold text-neutral-200 mt-0.5">{selectedComplaint.createdAt}</p>
                    </div>
                  </div>

                  {/* Message / Chat Thread area */}
                  <div className="flex-1 p-5 overflow-y-auto bg-neutral-950/80 space-y-4">
                    {/* Customer original complaint block */}
                    <div className="flex gap-3 items-start max-w-[85%]">
                      <Avatar className="h-8 w-8 bg-indigo-900 border border-indigo-700 flex-shrink-0 text-white flex items-center justify-center rounded-full">
                        <AvatarFallback className="text-xs bg-indigo-950 font-bold">KH</AvatarFallback>
                      </Avatar>
                      <div className="bg-neutral-800 border border-neutral-700/80 rounded-2xl rounded-tl-none p-3.5 shadow-sm text-sm">
                        <div className="flex items-center gap-2 mb-1.5">
                          <span className="font-semibold text-xs text-indigo-300">Khách hàng: {selectedComplaint.customerName}</span>
                          <span className="text-[10px] text-neutral-500 font-mono">{selectedComplaint.createdAt}</span>
                        </div>
                        <p className="text-neutral-100 leading-relaxed font-light">{selectedComplaint.content}</p>
                      </div>
                    </div>

                    {/* Chat replies */}
                    {selectedComplaint.responses.map((resp, idx) => {
                      const isAdminMsg = resp.from === "Admin";
                      const isOwnerMsg = resp.from === "Chủ sân";
                      return (
                        <div
                          key={idx}
                          className={`flex gap-3 items-start max-w-[85%] ${
                            isAdminMsg ? "ml-auto justify-end" : ""
                          }`}
                        >
                          {!isAdminMsg && (
                            <Avatar className={`h-8 w-8 flex-shrink-0 text-white flex items-center justify-center rounded-full ${
                              isOwnerMsg ? "bg-amber-900 border border-amber-700" : "bg-indigo-900 border border-indigo-700"
                            }`}>
                              <AvatarFallback className="text-[10px] bg-neutral-950 font-bold">
                                {isOwnerMsg ? "CS" : "KH"}
                              </AvatarFallback>
                            </Avatar>
                          )}

                          <div className={`p-3.5 rounded-2xl shadow-sm text-sm border ${
                            isAdminMsg
                              ? "bg-indigo-600/25 border-indigo-500/30 rounded-tr-none text-neutral-100"
                              : isOwnerMsg
                              ? "bg-amber-950/20 border-amber-700/30 rounded-tl-none text-neutral-100"
                              : "bg-neutral-800 border-neutral-700/80 rounded-tl-none text-neutral-100"
                          }`}>
                            <div className="flex items-center gap-2 mb-1.5">
                              <span className={`font-semibold text-xs ${
                                isAdminMsg ? "text-indigo-400" : isOwnerMsg ? "text-amber-400" : "text-indigo-300"
                              }`}>
                                {resp.from === "Admin" ? "Quản trị viên (Bạn)" : resp.from === "Chủ sân" ? `Chủ sân: ${selectedComplaint.ownerName}` : `Khách hàng: ${selectedComplaint.customerName}`}
                              </span>
                              <span className="text-[10px] text-neutral-500 font-mono">{resp.time}</span>
                            </div>
                            <p className="leading-relaxed font-light">{resp.message}</p>
                          </div>

                          {isAdminMsg && (
                            <Avatar className="h-8 w-8 bg-neutral-800 border border-neutral-700 flex-shrink-0 text-white flex items-center justify-center rounded-full">
                              <AvatarFallback className="text-[10px] bg-neutral-900 font-bold">AD</AvatarFallback>
                            </Avatar>
                          )}
                        </div>
                      );
                    })}

                    {/* Resolution status card */}
                    {selectedComplaint.status === "resolved" && (
                      <div className="flex justify-center my-6">
                        <div className="bg-emerald-950/25 border border-emerald-500/30 rounded-xl px-5 py-4 text-center max-w-[80%] shadow-lg">
                          <CheckCircle2 className="h-8 w-8 text-emerald-500 mx-auto mb-2" />
                          <h4 className="text-sm font-semibold text-emerald-400 mb-1">Khiếu nại này đã đóng & giải quyết</h4>
                          <p className="text-xs text-neutral-300 italic mb-2 leading-relaxed">
                            "{selectedComplaint.resolution}"
                          </p>
                          {selectedComplaint.resolvedAt && (
                            <p className="text-[10px] text-neutral-500 font-mono">Đóng vào: {selectedComplaint.resolvedAt}</p>
                          )}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Chat reply input area */}
                  {selectedComplaint.status !== "resolved" && (
                    <div className="p-4 border-t border-neutral-800 bg-neutral-900 flex gap-2 items-center">
                      <Input
                        placeholder="Nhập nội dung tin nhắn của Quản trị viên..."
                        value={replyMessage}
                        onChange={e => setReplyMessage(e.target.value)}
                        onKeyDown={e => {
                          if (e.key === 'Enter') handleSendMessage();
                        }}
                        className="bg-neutral-950 border-neutral-800 text-sm text-neutral-200 focus-visible:ring-indigo-600 focus-visible:ring-1"
                      />
                      <Button
                        size="icon"
                        onClick={handleSendMessage}
                        disabled={!replyMessage.trim()}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl h-10 w-10 flex items-center justify-center flex-shrink-0"
                      >
                        <Send className="h-4 w-4" />
                      </Button>
                    </div>
                  )}
                </div>
              ) : (
                <div className="flex-1 flex flex-col items-center justify-center p-8 text-neutral-500 bg-neutral-950/20">
                  <MessageSquare className="h-12 w-12 text-neutral-700 mb-3" />
                  <h3 className="font-semibold text-base text-neutral-400">Chưa chọn khiếu nại</h3>
                  <p className="text-xs text-neutral-500 mt-1 max-w-[280px] text-center">
                    Hãy bấm vào một khiếu nại ở danh sách bên trái để xem nội dung, xem lịch sử thảo luận và giải quyết tranh chấp.
                  </p>
                </div>
              )}
            </div>
          </div>
        </main>
      </div>

      {/* Resolve dialog */}
      <Dialog open={showResolveDialog} onOpenChange={setShowResolveDialog}>
        <DialogContent className="bg-neutral-900 border border-neutral-800 text-neutral-100 max-w-md rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-white text-base font-bold flex items-center gap-2">
              <CheckCircle2 className="h-5 w-5 text-indigo-500" />
              Đóng và giải quyết khiếu nại #{selectedComplaint?.complaintId}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-3">
            <p className="text-xs text-neutral-400 leading-relaxed">
              Bạn đang thực hiện đóng khiếu nại này với tư cách Quản trị viên. Nội dung giải pháp cuối cùng sẽ được ghi nhận và gửi thông báo đến cả Khách hàng và Chủ sân.
            </p>
            <div className="space-y-2">
              <label className="text-xs font-semibold text-neutral-300">Phương án giải quyết (Resolution)</label>
              <Textarea
                placeholder="Nhập chi tiết quyết định giải quyết của Admin (ví dụ: Hệ thống thực hiện hoàn tiền 100%, hoặc Yêu cầu chủ sân đền bù...)"
                value={resolutionText}
                onChange={e => setResolutionText(e.target.value)}
                rows={4}
                className="bg-neutral-950 border-neutral-800 text-sm text-neutral-200 focus-visible:ring-indigo-600 focus-visible:ring-1"
              />
            </div>
          </div>

          <div className="flex gap-2 justify-end pt-2">
            <Button variant="ghost" size="sm" onClick={() => setShowResolveDialog(false)} className="text-neutral-400 hover:text-white hover:bg-neutral-800">
              Hủy bỏ
            </Button>
            <Button
              size="sm"
              onClick={handleResolveComplaint}
              disabled={!resolutionText.trim()}
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-medium"
            >
              Xác nhận Đóng khiếu nại
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default AdminComplaintsPage;
