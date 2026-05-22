'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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
} from "lucide-react";

export function MatchRequestFeedPage() {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showJoinDialog, setShowJoinDialog] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<any>(null);

  const matchRequests = [
    {
      id: 1,
      user: {
        name: "Nguyễn Văn A",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
      },
      sportType: "Bóng đá",
      skillLevel: "Trung bình",
      date: "23/05/2024",
      time: "18:00",
      venue: "Sân bóng Thành Công",
      location: "Quận 1, TP.HCM",
      playersJoined: 3,
      playersNeeded: 5,
      description: "Tìm thêm 2 người chơi bóng tối thứ 7, cùng nhau vận động!",
    },
    {
      id: 2,
      user: {
        name: "Trần Thị B",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=2",
      },
      sportType: "Cầu lông",
      skillLevel: "Nâng cao",
      date: "24/05/2024",
      time: "19:00",
      venue: "Sân cầu lông Quận 3",
      location: "Quận 3, TP.HCM",
      playersJoined: 2,
      playersNeeded: 4,
      description: "Tìm đối thủ chơi đôi, level nâng cao nhé!",
    },
    {
      id: 3,
      user: {
        name: "Lê Văn C",
        avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=3",
      },
      sportType: "Bóng đá",
      skillLevel: "Mới bắt đầu",
      date: "25/05/2024",
      time: "17:00",
      venue: "Sân Vận Động Quận 7",
      location: "Quận 7, TP.HCM",
      playersJoined: 4,
      playersNeeded: 6,
      description: "Nhóm newbie tìm bạn chơi cùng, vui vẻ là chính!",
    },
  ];

  const getSkillLevelBadge = (level: string) => {
    const config = {
      "Mới bắt đầu": "bg-green-100 text-green-700",
      "Trung bình": "bg-blue-100 text-blue-700",
      "Nâng cao": "bg-purple-100 text-purple-700",
    };
    return config[level as keyof typeof config] || "";
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl">Tìm đối thủ</h1>
          <Button onClick={() => setShowCreateDialog(true)}>
            <Plus className="mr-2 h-5 w-5" />
            Tạo lời mời
          </Button>
        </div>

        {/* Filters */}
        <div className="flex gap-4 mb-6 flex-wrap">
          <div className="relative flex-1 min-w-64">
            <Search className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
            <Input placeholder="Tìm kiếm..." className="pl-10" />
          </div>

          <Select defaultValue="all">
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả môn</SelectItem>
              <SelectItem value="football">Bóng đá</SelectItem>
              <SelectItem value="badminton">Cầu lông</SelectItem>
              <SelectItem value="tennis">Quần vợt</SelectItem>
              <SelectItem value="basketball">Bóng rổ</SelectItem>
            </SelectContent>
          </Select>

          <Select defaultValue="all-level">
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all-level">Tất cả trình độ</SelectItem>
              <SelectItem value="beginner">Mới bắt đầu</SelectItem>
              <SelectItem value="intermediate">Trung bình</SelectItem>
              <SelectItem value="advanced">Nâng cao</SelectItem>
            </SelectContent>
          </Select>

          <Select defaultValue="all-date">
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all-date">Tất cả ngày</SelectItem>
              <SelectItem value="today">Hôm nay</SelectItem>
              <SelectItem value="tomorrow">Ngày mai</SelectItem>
              <SelectItem value="week">Tuần này</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Match Requests Feed */}
        <div className="grid grid-cols-1 gap-4">
          {matchRequests.map((request) => (
            <Card key={request.id} className="hover:shadow-lg transition-shadow">
              <CardContent className="p-6">
                <div className="flex gap-4">
                  {/* User Info */}
                  <Avatar className="h-12 w-12">
                    <AvatarImage src={request.user.avatar} />
                    <AvatarFallback>{request.user.name[0]}</AvatarFallback>
                  </Avatar>

                  <div className="flex-1">
                    {/* Header */}
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <h3 className="mb-1">{request.user.name}</h3>
                        <div className="flex gap-2">
                          <Badge variant="outline">{request.sportType}</Badge>
                          <Badge className={getSkillLevelBadge(request.skillLevel)}>
                            {request.skillLevel}
                          </Badge>
                        </div>
                      </div>
                      <Button
                        onClick={() => {
                          setSelectedRequest(request);
                          setShowJoinDialog(true);
                        }}
                      >
                        Tham gia
                      </Button>
                    </div>

                    {/* Description */}
                    <p className="mb-3 text-muted-foreground">
                      {request.description}
                    </p>

                    {/* Details */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                      <div className="flex items-center text-muted-foreground">
                        <Calendar className="h-4 w-4 mr-2" />
                        {request.date} - {request.time}
                      </div>

                      <div className="flex items-center text-muted-foreground">
                        <MapPin className="h-4 w-4 mr-2" />
                        {request.location}
                      </div>

                      <div className="flex items-center text-muted-foreground">
                        <Users className="h-4 w-4 mr-2" />
                        {request.playersJoined}/{request.playersNeeded} người
                      </div>

                      <div className="flex items-center text-muted-foreground">
                        <TrendingUp className="h-4 w-4 mr-2" />
                        {request.venue}
                      </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="mt-3">
                      <div className="w-full bg-muted rounded-full h-2">
                        <div
                          className="bg-primary h-2 rounded-full"
                          style={{
                            width: `${
                              (request.playersJoined / request.playersNeeded) * 100
                            }%`,
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
      </div>

      {/* Create Match Request Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Tạo lời mời tìm đối thủ</DialogTitle>
          </DialogHeader>

          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="sport">Môn thể thao *</Label>
                <Select>
                  <SelectTrigger id="sport">
                    <SelectValue placeholder="Chọn môn" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="football">Bóng đá</SelectItem>
                    <SelectItem value="badminton">Cầu lông</SelectItem>
                    <SelectItem value="tennis">Quần vợt</SelectItem>
                    <SelectItem value="basketball">Bóng rổ</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="skill">Trình độ *</Label>
                <Select>
                  <SelectTrigger id="skill">
                    <SelectValue placeholder="Chọn trình độ" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="beginner">Mới bắt đầu</SelectItem>
                    <SelectItem value="intermediate">Trung bình</SelectItem>
                    <SelectItem value="advanced">Nâng cao</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date">Ngày *</Label>
                <Input id="date" type="date" />
              </div>

              <div className="space-y-2">
                <Label htmlFor="time">Giờ *</Label>
                <Input id="time" type="time" />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="venue-select">Sân *</Label>
              <Select>
                <SelectTrigger id="venue-select">
                  <SelectValue placeholder="Chọn sân" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">Sân bóng Thành Công</SelectItem>
                  <SelectItem value="2">Arena Sports Center</SelectItem>
                  <SelectItem value="3">Sân Vận Động Quận 7</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="players">Số người cần *</Label>
              <Input
                id="players"
                type="number"
                min="1"
                max="20"
                placeholder="VD: 5"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="desc">Mô tả</Label>
              <Textarea
                id="desc"
                placeholder="Mô tả về buổi chơi, yêu cầu..."
                rows={3}
              />
            </div>

            <div className="flex gap-2">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => setShowCreateDialog(false)}
              >
                Hủy
              </Button>
              <Button className="flex-1">Tạo lời mời</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Join Match Request Dialog */}
      <Dialog open={showJoinDialog} onOpenChange={setShowJoinDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tham gia kèo</DialogTitle>
          </DialogHeader>

          {selectedRequest && (
            <div className="space-y-4">
              <Card>
                <CardContent className="p-4">
                  <h3 className="mb-2">{selectedRequest.description}</h3>
                  <div className="space-y-2 text-sm text-muted-foreground">
                    <div>
                      Môn: <strong>{selectedRequest.sportType}</strong>
                    </div>
                    <div>
                      Ngày: <strong>{selectedRequest.date}</strong> - {selectedRequest.time}
                    </div>
                    <div>
                      Sân: <strong>{selectedRequest.venue}</strong>
                    </div>
                    <div>
                      Số người: <strong>{selectedRequest.playersJoined}/{selectedRequest.playersNeeded}</strong>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <div className="space-y-2">
                <Label htmlFor="join-note">Lời nhắn (không bắt buộc)</Label>
                <Textarea
                  id="join-note"
                  placeholder="Giới thiệu bản thân, trình độ..."
                  rows={3}
                />
              </div>

              <div className="flex gap-2">
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => setShowJoinDialog(false)}
                >
                  Hủy
                </Button>
                <Button className="flex-1">Gửi yêu cầu tham gia</Button>
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
