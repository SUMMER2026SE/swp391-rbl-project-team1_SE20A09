'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Bot, Send, Search, Calendar, TrendingUp, MapPin } from "lucide-react";

function AIAssistantPage() {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([
    {
      id: 1,
      type: "assistant",
      content:
        "Xin chào! Tôi là trợ lý AI của SportHub. Tôi có thể giúp bạn tìm sân, đặt lịch, hoặc gợi ý sân phù hợp. Bạn cần tôi giúp gì?",
      timestamp: "14:00",
    },
  ]);

  const quickActions = [
    { icon: <Search className="h-4 w-4" />, text: "Tìm sân gần tôi" },
    { icon: <Calendar className="h-4 w-4" />, text: "Xem lịch đặt sân" },
    { icon: <TrendingUp className="h-4 w-4" />, text: "Gợi ý sân cho tôi" },
  ];

  const venueRecommendation = {
    name: "Sân bóng Thành Công",
    image:
      "https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
    sportType: "Bóng đá",
    price: 500000,
    rating: 4.8,
    location: "Quận 1, TP.HCM",
    availableSlots: "18:00 - 20:00",
  };

  const handleSend = () => {
    if (!message.trim()) return;

    const newMessage = {
      id: messages.length + 1,
      type: "user",
      content: message,
      timestamp: new Date().toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
      }),
    };

    setMessages([...messages, newMessage]);
    setMessage("");

    // Simulate AI response
    setTimeout(() => {
      const aiResponse = {
        id: messages.length + 2,
        type: "assistant",
        content: "Để tôi tìm sân phù hợp cho bạn...",
        timestamp: new Date().toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
      };
      setMessages((prev) => [...prev, aiResponse]);
    }, 1000);
  };

  return (
    <div className="h-screen flex flex-col bg-background">
      <Header />

      <div className="flex-1 container mx-auto px-4 py-8 overflow-hidden flex flex-col">
        <div className="mb-6 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full mb-3">
            <Bot className="h-8 w-8 text-primary" />
          </div>
          <h1 className="text-2xl mb-2">Trợ lý AI đặt sân</h1>
          <p className="text-muted-foreground">
            Hỏi tôi bất cứ điều gì về đặt sân và tìm kiếm
          </p>
        </div>

        <Card className="flex-1 flex flex-col overflow-hidden">
          <ScrollArea className="flex-1 p-6">
            <div className="space-y-6">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex gap-3 ${
                    msg.type === "user" ? "flex-row-reverse" : ""
                  }`}
                >
                  <Avatar className="h-10 w-10 flex-shrink-0">
                    {msg.type === "assistant" ? (
                      <div className="w-full h-full bg-primary/10 flex items-center justify-center">
                        <Bot className="h-6 w-6 text-primary" />
                      </div>
                    ) : (
                      <AvatarFallback>U</AvatarFallback>
                    )}
                  </Avatar>

                  <div
                    className={`flex-1 ${
                      msg.type === "user" ? "text-right" : ""
                    }`}
                  >
                    <div
                      className={`inline-block max-w-md md:max-w-lg ${
                        msg.type === "user"
                          ? "bg-primary text-primary-foreground"
                          : "bg-muted"
                      } rounded-lg px-4 py-3`}
                    >
                      <p className="text-sm">{msg.content}</p>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {msg.timestamp}
                    </p>
                  </div>
                </div>
              ))}

              {/* Quick Actions (shown on first message) */}
              {messages.length === 1 && (
                <div className="flex flex-wrap gap-2 justify-center">
                  {quickActions.map((action, idx) => (
                    <Button
                      key={idx}
                      variant="outline"
                      size="sm"
                      className="gap-2"
                      onClick={() => setMessage(action.text)}
                    >
                      {action.icon}
                      {action.text}
                    </Button>
                  ))}
                </div>
              )}

              {/* Venue Recommendation Card */}
              {messages.length > 2 && (
                <div className="flex justify-center">
                  <Card className="max-w-sm">
                    <CardContent className="p-4">
                      <div className="flex gap-3 mb-3">
                        <img
                          src={venueRecommendation.image}
                          alt={venueRecommendation.name}
                          className="w-20 h-20 rounded-lg object-cover"
                        />
                        <div className="flex-1">
                          <h4 className="mb-1">{venueRecommendation.name}</h4>
                          <Badge variant="outline" className="mb-2">
                            {venueRecommendation.sportType}
                          </Badge>
                          <div className="flex items-center text-sm text-muted-foreground">
                            <MapPin className="h-3 w-3 mr-1" />
                            {venueRecommendation.location}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center justify-between text-sm mb-3">
                        <span className="text-muted-foreground">
                          Khung giờ trống
                        </span>
                        <span>{venueRecommendation.availableSlots}</span>
                      </div>

                      <div className="flex items-center justify-between mb-3">
                        <span className="text-xl text-primary">
                          {venueRecommendation.price.toLocaleString('vi-VN')}đ
                        </span>
                        <span className="text-sm">
                          ⭐ {venueRecommendation.rating}
                        </span>
                      </div>

                      <Button className="w-full" size="sm">
                        Chọn sân này
                      </Button>
                    </CardContent>
                  </Card>
                </div>
              )}
            </div>
          </ScrollArea>

          <div className="p-4 border-t">
            <div className="flex gap-2">
              <Input
                placeholder="Hỏi tôi về sân, giá, lịch trống..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === "Enter") handleSend();
                }}
              />
              <Button onClick={handleSend} disabled={!message.trim()}>
                <Send className="h-5 w-5" />
              </Button>
            </div>
          </div>
        </Card>
      </div>

      <Footer />
    </div>
  );
}

export default AIAssistantPage;
