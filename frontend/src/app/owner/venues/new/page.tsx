'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Upload, X, Plus, Minus, MapPin } from "lucide-react";

function AddEditVenuePage() {
  const [currentStep, setCurrentStep] = useState(1);
  const [uploadedPhotos, setUploadedPhotos] = useState<string[]>([]);
  const [timeSlots, setTimeSlots] = useState([
    { time: "06:00 - 08:00", price: 500000, available: true },
    { time: "08:00 - 10:00", price: 500000, available: true },
  ]);
  const [accessories, setAccessories] = useState([
    { name: "Bóng đá", price: 50000, quantity: 10 },
  ]);

  const steps = [
    "Thông tin cơ bản",
    "Hình ảnh",
    "Giá & Lịch trống",
    "Phụ kiện",
  ];

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-3xl mb-8">Thêm sân mới</h1>

          {/* Progress */}
          <div className="mb-8">
            <div className="flex items-center justify-between mb-3">
              {steps.map((step, idx) => (
                <div
                  key={idx}
                  className={`flex-1 text-center ${
                    idx + 1 === currentStep
                      ? "text-primary"
                      : idx + 1 < currentStep
                      ? "text-green-600"
                      : "text-muted-foreground"
                  }`}
                >
                  <div
                    className={`w-10 h-10 rounded-full mx-auto mb-2 flex items-center justify-center ${
                      idx + 1 === currentStep
                        ? "bg-primary text-primary-foreground"
                        : idx + 1 < currentStep
                        ? "bg-green-600 text-white"
                        : "bg-muted"
                    }`}
                  >
                    {idx + 1}
                  </div>
                  <div className="text-sm">{step}</div>
                </div>
              ))}
            </div>
            <Progress value={(currentStep / steps.length) * 100} />
          </div>

          {/* Step 1: Basic Info */}
          {currentStep === 1 && (
            <Card>
              <CardHeader>
                <h3>Thông tin cơ bản</h3>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="venue-name">Tên sân *</Label>
                  <Input id="venue-name" placeholder="VD: Sân bóng Thành Công" />
                </div>

                <div className="space-y-2">
                  <Label>Loại môn thể thao *</Label>
                  <div className="flex flex-wrap gap-2">
                    {["Bóng đá", "Cầu lông", "Quần vợt", "Bóng rổ"].map((sport) => (
                      <Badge
                        key={sport}
                        variant="outline"
                        className="cursor-pointer hover:bg-primary hover:text-primary-foreground"
                      >
                        {sport}
                      </Badge>
                    ))}
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="address">Địa chỉ *</Label>
                  <div className="relative">
                    <MapPin className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
                    <Input
                      id="address"
                      placeholder="123 Đường ABC, Quận 1, TP.HCM"
                      className="pl-10"
                    />
                  </div>
                  <Button variant="link" size="sm" className="p-0 h-auto">
                    Chọn trên bản đồ
                  </Button>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Mô tả</Label>
                  <Textarea
                    id="description"
                    placeholder="Mô tả chi tiết về sân..."
                    rows={4}
                  />
                </div>

                <div className="space-y-2">
                  <Label>Tiện ích</Label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      "Bãi đỗ xe",
                      "Phòng thay đồ",
                      "Đèn chiếu sáng",
                      "Wifi",
                      "Căng tin",
                      "Nhà vệ sinh",
                    ].map((amenity) => (
                      <div key={amenity} className="flex items-center space-x-2">
                        <Checkbox id={amenity} />
                        <label htmlFor={amenity} className="text-sm cursor-pointer">
                          {amenity}
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Step 2: Photos */}
          {currentStep === 2 && (
            <Card>
              <CardHeader>
                <h3>Hình ảnh sân</h3>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-3 gap-4">
                  {uploadedPhotos.map((photo, idx) => (
                    <div key={idx} className="relative aspect-square">
                      <img
                        src={photo}
                        alt={`Photo ${idx + 1}`}
                        className="w-full h-full object-cover rounded-lg"
                      />
                      {idx === 0 && (
                        <Badge className="absolute top-2 left-2">Ảnh chính</Badge>
                      )}
                      <button
                        className="absolute top-2 right-2 bg-destructive text-destructive-foreground rounded-full p-1"
                        onClick={() =>
                          setUploadedPhotos(uploadedPhotos.filter((_, i) => i !== idx))
                        }
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  ))}

                  <button className="aspect-square border-2 border-dashed rounded-lg flex flex-col items-center justify-center text-muted-foreground hover:border-primary hover:text-primary transition-colors">
                    <Upload className="h-8 w-8 mb-2" />
                    <span className="text-sm">Tải ảnh lên</span>
                  </button>
                </div>

                <p className="text-sm text-muted-foreground">
                  * Tải lên tối thiểu 3 ảnh. Ảnh đầu tiên sẽ là ảnh chính.
                </p>
              </CardContent>
            </Card>
          )}

          {/* Step 3: Pricing & Slots */}
          {currentStep === 3 && (
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3>Giá & Khung giờ</h3>
                  <Button
                    size="sm"
                    onClick={() =>
                      setTimeSlots([
                        ...timeSlots,
                        { time: "", price: 0, available: true },
                      ])
                    }
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Thêm khung giờ
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {timeSlots.map((slot, idx) => (
                    <div
                      key={idx}
                      className="flex items-center gap-3 p-3 border rounded-lg"
                    >
                      <Input
                        placeholder="VD: 06:00 - 08:00"
                        value={slot.time}
                        className="flex-1"
                      />
                      <Input
                        type="number"
                        placeholder="Giá"
                        value={slot.price}
                        className="w-40"
                      />
                      <span className="text-sm text-muted-foreground">đ/giờ</span>
                      <Checkbox checked={slot.available} />
                      <Label className="text-sm">Khả dụng</Label>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() =>
                          setTimeSlots(timeSlots.filter((_, i) => i !== idx))
                        }
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Step 4: Accessories */}
          {currentStep === 4 && (
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3>Phụ kiện cho thuê</h3>
                  <Button
                    size="sm"
                    onClick={() =>
                      setAccessories([
                        ...accessories,
                        { name: "", price: 0, quantity: 0 },
                      ])
                    }
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Thêm phụ kiện
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {accessories.map((accessory, idx) => (
                    <div
                      key={idx}
                      className="flex items-center gap-3 p-3 border rounded-lg"
                    >
                      <Input
                        placeholder="Tên phụ kiện"
                        value={accessory.name}
                        className="flex-1"
                      />
                      <Input
                        type="number"
                        placeholder="Giá"
                        value={accessory.price}
                        className="w-32"
                      />
                      <span className="text-sm text-muted-foreground">đ</span>
                      <Input
                        type="number"
                        placeholder="SL"
                        value={accessory.quantity}
                        className="w-24"
                      />
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() =>
                          setAccessories(accessories.filter((_, i) => i !== idx))
                        }
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Navigation */}
          <div className="flex justify-between mt-6">
            <Button
              variant="outline"
              onClick={() => setCurrentStep(Math.max(1, currentStep - 1))}
              disabled={currentStep === 1}
            >
              Quay lại
            </Button>

            {currentStep < steps.length ? (
              <Button onClick={() => setCurrentStep(currentStep + 1)}>
                Tiếp tục
              </Button>
            ) : (
              <Button>Hoàn tất</Button>
            )}
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default AddEditVenuePage;
