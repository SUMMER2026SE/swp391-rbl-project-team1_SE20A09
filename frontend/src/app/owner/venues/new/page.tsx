'use client'

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Upload, X, MapPin, Loader2 } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { stadiumService } from "@/lib/services/stadium";
import { SportType } from "@/types/stadium";
import { uploadStadiumImage } from "@/lib/api";
import { toast } from "sonner";

const stadiumSchema = z.object({
  stadiumName: z.string().min(3, "Tên sân phải có ít nhất 3 ký tự").max(100),
  address: z.string().min(5, "Địa chỉ không hợp lệ"),
  sportTypeId: z.number({
    required_error: "Vui lòng chọn môn thể thao",
    invalid_type_error: "Vui lòng chọn môn thể thao",
  }).min(1, "Vui lòng chọn môn thể thao"),
  pricePerHour: z.number({ invalid_type_error: "Vui lòng nhập giá" }).min(1000, "Giá tối thiểu là 1,000đ"),
  description: z.string().optional(),
  openTime: z.string().optional(),
  closeTime: z.string().optional(),
}).refine(
  (data) => {
    if (!data.openTime || !data.closeTime) return true;
    return data.closeTime > data.openTime;
  },
  { message: "Giờ đóng cửa phải sau giờ mở cửa", path: ["closeTime"] }
);

type StadiumFormValues = z.infer<typeof stadiumSchema>;

function AddVenuePage() {
  const router = useRouter();
  const { data: session, status } = useSession();
  const [currentStep, setCurrentStep] = useState(1);
  const [sportTypes, setSportTypes] = useState<SportType[]>([]);
  const [uploadedPhotos, setUploadedPhotos] = useState<string[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<StadiumFormValues>({
    resolver: zodResolver(stadiumSchema),
    defaultValues: {
      stadiumName: "",
      address: "",
      pricePerHour: 100000,
      description: "",
      openTime: "06:00",
      closeTime: "22:00",
    },
  });

  // Auth guard — redirect to login if not authenticated
  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login");
    }
  }, [status, router]);

  useEffect(() => {
    stadiumService.getSportTypes()
      .then(setSportTypes)
      .catch(() => toast.error("Không thể tải danh sách môn thể thao"));
  }, []);

  if (status === "loading") {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!session) return null;

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    setIsUploading(true);
    let successCount = 0;
    for (const file of Array.from(files)) {
      try {
        const result = await uploadStadiumImage(file);
        setUploadedPhotos(prev => [...prev, result.url]);
        successCount++;
      } catch {
        toast.error(`Không thể tải lên ảnh "${file.name}". Bỏ qua và tiếp tục.`);
      }
    }
    if (successCount > 0) toast.success(`Đã tải lên ${successCount} ảnh`);
    setIsUploading(false);
  };

  const onSubmit = async (data: StadiumFormValues) => {
    if (uploadedPhotos.length < 1) {
      toast.error("Vui lòng tải lên ít nhất 1 ảnh");
      setCurrentStep(2);
      return;
    }
    setIsSubmitting(true);
    try {
      // Normalize HH:mm → HH:mm:ss for backend LocalTime deserialization
      const normalizeTime = (t?: string) => {
        if (!t) return undefined;
        // Already HH:mm:ss
        if (/^\d{2}:\d{2}:\d{2}$/.test(t)) return t;
        // HH:mm → append :00
        return `${t}:00`;
      };
      const openTime = normalizeTime(data.openTime);
      const closeTime = normalizeTime(data.closeTime);
      await stadiumService.createStadium({ ...data, openTime, closeTime, imageUrls: uploadedPhotos });
      toast.success("Thêm sân thành công!");
      router.push("/owner/venues");
    } catch (error: any) {
      toast.error(error.message || "Đã xảy ra lỗi khi thêm sân");
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = ["Thông tin cơ bản", "Hình ảnh", "Giá & Giờ mở cửa"];

  const handleNext = async () => {
    let valid = false;
    if (currentStep === 1) {
      valid = await form.trigger(["stadiumName", "address", "sportTypeId"]);
    } else if (currentStep === 2) {
      valid = true;
    }
    if (valid) setCurrentStep(prev => prev + 1);
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold mb-8">Thêm sân mới</h1>

          {/* Progress */}
          <div className="mb-8">
            <div className="flex items-center justify-between mb-3">
              {steps.map((step, idx) => (
                <div
                  key={idx}
                  className={`flex-1 text-center ${idx + 1 === currentStep ? "text-primary font-medium" : "text-muted-foreground"}`}
                >
                  <div
                    className={`w-10 h-10 rounded-full mx-auto mb-2 flex items-center justify-center border-2 ${
                      idx + 1 <= currentStep ? "bg-primary text-white border-primary" : "bg-muted border-muted"
                    }`}
                  >
                    {idx + 1}
                  </div>
                  <div className="text-xs sm:text-sm">{step}</div>
                </div>
              ))}
            </div>
            <Progress value={(currentStep / steps.length) * 100} className="h-2" />
          </div>

          {/* Dùng div thay vì form để tránh browser auto-submit khi nhấn Enter */}
          <div>
            {/* Step 1: Basic Info */}
            {currentStep === 1 && (
              <Card>
                <CardHeader>
                  <h2 className="text-xl font-semibold">Thông tin cơ bản</h2>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="stadiumName">Tên sân *</Label>
                    <Input
                      {...form.register("stadiumName")}
                      id="stadiumName"
                      placeholder="VD: Sân bóng Thành Công"
                    />
                    {form.formState.errors.stadiumName && (
                      <p className="text-sm text-destructive">{form.formState.errors.stadiumName.message}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label>Loại môn thể thao *</Label>
                    <Select onValueChange={(val) => form.setValue("sportTypeId", parseInt(val), { shouldValidate: true })}>
                      <SelectTrigger>
                        <SelectValue placeholder="Chọn môn thể thao" />
                      </SelectTrigger>
                      <SelectContent>
                        {sportTypes.map((type) => (
                          <SelectItem key={type.sportTypeId} value={type.sportTypeId.toString()}>
                            {type.sportName}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {form.formState.errors.sportTypeId && (
                      <p className="text-sm text-destructive">{form.formState.errors.sportTypeId.message}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="address">Địa chỉ *</Label>
                    <div className="relative">
                      <MapPin className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
                      <Input
                        {...form.register("address")}
                        id="address"
                        placeholder="Số 123, Đường ABC, Quận X, TP.HCM"
                        className="pl-10"
                      />
                    </div>
                    {form.formState.errors.address && (
                      <p className="text-sm text-destructive">{form.formState.errors.address.message}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="description">Mô tả</Label>
                    <Textarea
                      {...form.register("description")}
                      id="description"
                      placeholder="Mô tả chi tiết về sân, tiện ích, quy định..."
                      rows={4}
                    />
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Step 2: Photos */}
            {currentStep === 2 && (
              <Card>
                <CardHeader>
                  <h2 className="text-xl font-semibold">Hình ảnh sân</h2>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                    {uploadedPhotos.map((photo, idx) => (
                      <div key={idx} className="relative aspect-video">
                        <img
                          src={photo}
                          alt={`Photo ${idx + 1}`}
                          className="w-full h-full object-cover rounded-lg border"
                        />
                        {idx === 0 && (
                          <Badge className="absolute top-2 left-2">Ảnh chính</Badge>
                        )}
                        <button
                          type="button"
                          className="absolute top-2 right-2 bg-destructive text-destructive-foreground rounded-full p-1 shadow-sm"
                          onClick={() => setUploadedPhotos(uploadedPhotos.filter((_, i) => i !== idx))}
                        >
                          <X className="h-4 w-4" />
                        </button>
                      </div>
                    ))}

                    <label className="aspect-video border-2 border-dashed rounded-lg flex flex-col items-center justify-center text-muted-foreground hover:border-primary hover:text-primary cursor-pointer transition-colors bg-muted/50">
                      {isUploading ? (
                        <Loader2 className="h-8 w-8 animate-spin" />
                      ) : (
                        <>
                          <Upload className="h-8 w-8 mb-2" />
                          <span className="text-xs sm:text-sm">Tải ảnh lên</span>
                        </>
                      )}
                      <input
                        type="file"
                        multiple
                        accept="image/*"
                        className="hidden"
                        onChange={handlePhotoUpload}
                        disabled={isUploading}
                      />
                    </label>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    * Tải lên tối thiểu 1 ảnh. Nên dùng ảnh ngang (16:9) để hiển thị tốt nhất.
                  </p>
                </CardContent>
              </Card>
            )}

            {/* Step 3: Pricing & Open Time */}
            {currentStep === 3 && (
              <Card>
                <CardHeader>
                  <h2 className="text-xl font-semibold">Giá & Thời gian hoạt động</h2>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="space-y-2">
                    <Label htmlFor="pricePerHour">Giá thuê mỗi giờ (VNĐ) *</Label>
                    <Input
                      {...form.register("pricePerHour", { valueAsNumber: true })}
                      id="pricePerHour"
                      type="number"
                      placeholder="VD: 200000"
                    />
                    {form.formState.errors.pricePerHour && (
                      <p className="text-sm text-destructive">{form.formState.errors.pricePerHour.message}</p>
                    )}
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="openTime">Giờ mở cửa</Label>
                      <Input
                        {...form.register("openTime")}
                        id="openTime"
                        type="time"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="closeTime">Giờ đóng cửa</Label>
                      <Input
                        {...form.register("closeTime")}
                        id="closeTime"
                        type="time"
                      />
                      {form.formState.errors.closeTime && (
                        <p className="text-sm text-destructive">{form.formState.errors.closeTime.message}</p>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Navigation */}
            <div className="flex justify-between mt-8">
              <Button
                type="button"
                variant="outline"
                onClick={() => setCurrentStep(prev => prev - 1)}
                disabled={currentStep === 1 || isSubmitting}
              >
                Quay lại
              </Button>

              {currentStep < steps.length ? (
                <Button type="button" onClick={handleNext}>
                  Tiếp tục
                </Button>
              ) : (
                <Button
                  type="button"
                  disabled={isSubmitting}
                  onClick={form.handleSubmit(onSubmit)}
                >
                  {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  Hoàn tất & Thêm sân
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default AddVenuePage;
