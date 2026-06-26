'use client'

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { useSession } from "next-auth/react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Loader2, ArrowLeft, Upload, X } from "lucide-react";
import Image from "next/image";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { stadiumService } from "@/lib/services/stadium";
import { SportType, StadiumResponse } from "@/types/stadium";
import { uploadStadiumImage } from "@/lib/api";
import { toast } from "sonner";
import { AddressPicker } from "@/components/AddressPicker";
import { Badge } from "@/components/ui/badge";

const updateStadiumSchema = z.object({
  stadiumName: z.string()
    .min(3, "Tên sân phải có ít nhất 3 ký tự")
    .max(100, "Tên sân không được quá 100 ký tự"),
  address: z.string()
    .min(5, "Địa chỉ phải có ít nhất 5 ký tự")
    .max(500, "Địa chỉ không được quá 500 ký tự"),
  latitude: z.number({ message: "Vui lòng chọn vị trí trên bản đồ" })
    .min(-90, "Vĩ độ không hợp lệ")
    .max(90, "Vĩ độ không hợp lệ"),
  longitude: z.number({ message: "Vui lòng chọn vị trí trên bản đồ" })
    .min(-180, "Kinh độ không hợp lệ")
    .max(180, "Kinh độ không hợp lệ"),
  sportTypeId: z.number({ message: "Vui lòng chọn môn thể thao" })
    .min(1, "Vui lòng chọn môn thể thao"),
  pricePerHour: z.number({ message: "Vui lòng nhập giá" })
    .min(1000, "Giá tối thiểu là 1,000đ")
    .max(99999999.99, "Giá không được vượt quá 99,999,999.99đ"),
  description: z.string().max(2000, "Mô tả không được quá 2000 ký tự").optional(),
  openTime: z.string().min(1, "Vui lòng chọn giờ mở cửa"),
  closeTime: z.string().min(1, "Vui lòng chọn giờ đóng cửa"),
}).refine(
  (data) => {
    if (!data.openTime || !data.closeTime) return true;
    return data.closeTime > data.openTime;
  },
  { message: "Giờ đóng cửa phải sau giờ mở cửa", path: ["closeTime"] }
);

type UpdateStadiumFormValues = z.infer<typeof updateStadiumSchema>;

export default function EditVenuePage() {
  const router = useRouter();
  const params = useParams();
  const stadiumId = parseInt(params.id as string);
  const { data: session, status } = useSession();
  const [sportTypes, setSportTypes] = useState<SportType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [stadium, setStadium] = useState<StadiumResponse | null>(null);
  const [uploadedPhotos, setUploadedPhotos] = useState<string[]>([]);
  const [isUploading, setIsUploading] = useState(false);

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    if (uploadedPhotos.length + files.length > 10) {
      toast.error("Chỉ được tải lên tối đa 10 ảnh");
      e.target.value = "";
      return;
    }
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

  const form = useForm<UpdateStadiumFormValues>({
    resolver: zodResolver(updateStadiumSchema),
    defaultValues: {
      stadiumName: "",
      address: "",
      latitude: 16.0544,
      longitude: 108.2022,
      pricePerHour: 100000,
      description: "",
      openTime: "06:00",
      closeTime: "22:00",
    },
  });


  useEffect(() => {
    if (status === "authenticated") {
      Promise.all([
        stadiumService.getSportTypes(),
        stadiumService.getStadiumById(stadiumId)
      ])
        .then(([types, stadiumData]) => {
          setSportTypes(types);
          setStadium(stadiumData);
          setUploadedPhotos(stadiumData.imageUrls || []);
          const normalizeTime = (t: string) => {
            if (!t) return "06:00";
            if (/^\d{2}:\d{2}:\d{2}$/.test(t)) return t.substring(0, 5);
            return t;
          };
          form.reset({
            stadiumName: stadiumData.stadiumName,
            address: stadiumData.address,
            latitude: stadiumData.latitude ?? 16.0544,
            longitude: stadiumData.longitude ?? 108.2022,
            sportTypeId: stadiumData.sportTypeId,
            pricePerHour: stadiumData.pricePerHour,
            description: stadiumData.description || "",
            openTime: normalizeTime(stadiumData.openTime),
            closeTime: normalizeTime(stadiumData.closeTime),
          });
        })
        .catch((err) => {
          toast.error(err.message || "Không thể tải thông tin sân");
          router.push("/owner/venues");
        })
        .finally(() => setIsLoading(false));
    }
  }, [status, stadiumId, form, router]);

  if (status === "loading" || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!session || !stadium) return null;

  const onSubmit = async (data: UpdateStadiumFormValues) => {
    if (uploadedPhotos.length < 1) {
      toast.error("Vui lòng tải lên ít nhất 1 ảnh");
      return;
    }
    setIsSubmitting(true);
    try {
      const normalizeTime = (t: string) => {
        if (/^\d{2}:\d{2}:\d{2}$/.test(t)) return t;
        return `${t}:00`;
      };
      const payload = {
        ...data,
        openTime: normalizeTime(data.openTime),
        closeTime: normalizeTime(data.closeTime),
        imageUrls: uploadedPhotos,
      };
      await stadiumService.updateStadium(stadiumId, payload);
      toast.success("Cập nhật sân thành công!");
      router.push("/owner/venues");
    } catch (error: any) {
      toast.error(error.message || "Đã xảy ra lỗi khi cập nhật sân");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <Button
          variant="ghost"
          onClick={() => router.push("/owner/venues")}
          className="mb-4"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>

        <h1 className="text-3xl font-bold mb-8">Chỉnh sửa thông tin sân</h1>

        <form onSubmit={form.handleSubmit(onSubmit)}>
          <Card>
            <CardHeader>
              <h2 className="text-xl font-semibold">Thông tin sân</h2>
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
                <Select
                  value={form.watch("sportTypeId")?.toString()}
                  onValueChange={(val) => form.setValue("sportTypeId", parseInt(val), { shouldValidate: true })}
                >
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
                <AddressPicker
                  initialAddress={stadium.address}
                  initialLat={stadium.latitude}
                  initialLng={stadium.longitude}
                  onAddressChange={(data) => {
                    form.setValue("address", data.addressText, { shouldValidate: true });
                    form.setValue("latitude", data.lat, { shouldValidate: true });
                    form.setValue("longitude", data.lng, { shouldValidate: true });
                  }}
                />
                {form.formState.errors.address && (
                  <p className="text-sm text-destructive">{form.formState.errors.address.message}</p>
                )}
                {form.formState.errors.latitude && (
                  <p className="text-sm text-destructive">{form.formState.errors.latitude.message}</p>
                )}
                {form.formState.errors.longitude && (
                  <p className="text-sm text-destructive">{form.formState.errors.longitude.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Mô tả</Label>
                <Textarea
                  {...form.register("description")}
                  id="description"
                  className="min-h-[100px]"
                  placeholder="Mô tả chi tiết về sân"
                />
                {form.formState.errors.description && (
                  <p className="text-sm text-destructive">{form.formState.errors.description.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label className="text-base font-semibold">Hình ảnh sân *</Label>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                  {uploadedPhotos.map((photo, idx) => (
                    <div key={idx} className="relative aspect-video">
                      <Image
                        src={photo}
                        alt={`Photo ${idx + 1}`}
                        fill
                        className="object-cover rounded-lg border"
                        unoptimized
                      />
                      {idx === 0 && (
                        <Badge className="absolute top-2 left-2 bg-primary text-white">Ảnh chính</Badge>
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
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="pricePerHour">Giá thuê (VNĐ/giờ) *</Label>
                  <Input
                    {...form.register("pricePerHour", { valueAsNumber: true })}
                    id="pricePerHour"
                    type="number"
                    min="1000"
                    step="1000"
                  />
                  {form.formState.errors.pricePerHour && (
                    <p className="text-sm text-destructive">{form.formState.errors.pricePerHour.message}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="openTime">Giờ mở cửa *</Label>
                  <Input
                    {...form.register("openTime")}
                    id="openTime"
                    type="time"
                  />
                  {form.formState.errors.openTime && (
                    <p className="text-sm text-destructive">{form.formState.errors.openTime.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="closeTime">Giờ đóng cửa *</Label>
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

              <div className="flex justify-end gap-3 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => router.push("/owner/venues")}
                  disabled={isSubmitting}
                >
                  Hủy
                </Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Đang lưu...
                    </>
                  ) : (
                    "Lưu thay đổi"
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </form>
      </div>
    </div>
  );
}
