'use client'

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Checkbox } from "@/components/ui/checkbox";
import { Upload, X, MapPin, Loader2 } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { stadiumService } from "@/lib/services/stadium";
import { SportType } from "@/types/stadium";
import { getAmenities, Amenity } from "@/lib/api/stadium";
import { uploadStadiumImage } from "@/lib/api";
import { toast } from "sonner";
import { AddressPicker } from "@/components/AddressPicker";

const complexSchema = z.object({
  name: z.string()
    .min(3, "Tên tổ hợp phải có ít nhất 3 ký tự")
    .max(150, "Tên tổ hợp không được quá 150 ký tự"),
  address: z.string()
    .min(5, "Địa chỉ phải có ít nhất 5 ký tự")
    .max(500, "Địa chỉ không được quá 500 ký tự"),
  phone: z.string()
    .max(20, "Số điện thoại không được quá 20 ký tự")
    .optional()
    .or(z.literal("")),
  latitude: z.number({ message: "Vui lòng chọn vị trí trên bản đồ" })
    .min(-90, "Vĩ độ không hợp lệ")
    .max(90, "Vĩ độ không hợp lệ"),
  longitude: z.number({ message: "Vui lòng chọn vị trí trên bản đồ" })
    .min(-180, "Kinh độ không hợp lệ")
    .max(180, "Kinh độ không hợp lệ"),
  description: z.string().max(2000, "Mô tả không được quá 2000 ký tự").optional(),
  sportTypeIds: z.array(z.number()).min(1, "Vui lòng chọn ít nhất 1 môn thể thao"),
  amenityIds: z.array(z.number()),
});

type ComplexFormValues = z.infer<typeof complexSchema>;

function AddVenuePage() {
  const router = useRouter();
  const { data: session, status } = useSession();
  const [currentStep, setCurrentStep] = useState(1);
  const [sportTypes, setSportTypes] = useState<SportType[]>([]);
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [uploadedPhotos, setUploadedPhotos] = useState<string[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<ComplexFormValues>({
    resolver: zodResolver(complexSchema),
    defaultValues: {
      name: "",
      address: "",
      phone: "",
      latitude: 16.0544,
      longitude: 108.2022,
      description: "",
      sportTypeIds: [],
      amenityIds: [],
    },
  });

  useEffect(() => {
    Promise.all([
      stadiumService.getSportTypes(),
      getAmenities()
    ])
      .then(([sports, amens]) => {
        setSportTypes(sports);
        setAmenities(amens);
      })
      .catch(() => toast.error("Không thể tải danh sách môn thể thao và tiện ích"));
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

  const onSubmit = async (data: ComplexFormValues) => {
    if (uploadedPhotos.length < 1) {
      toast.error("Vui lòng tải lên ít nhất 1 ảnh");
      setCurrentStep(2);
      return;
    }
    setIsSubmitting(true);
    try {
      await stadiumService.createComplex({
        name: data.name.trim(),
        address: data.address.trim(),
        phone: data.phone?.trim() || undefined,
        latitude: data.latitude,
        longitude: data.longitude,
        description: data.description?.trim() || undefined,
        sportTypeIds: data.sportTypeIds,
        amenityIds: data.amenityIds,
        coverImageUrl: uploadedPhotos[0],
        imageUrls: uploadedPhotos,
      });
      toast.success("Đăng ký tổ hợp sân thành công! Đang chờ Admin phê duyệt.");
      router.push("/owner/venues");
    } catch (error: any) {
      toast.error(error.message || "Đã xảy ra lỗi khi thêm tổ hợp");
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = ["Thông tin cơ bản", "Hình ảnh", "Môn thể thao & Tiện ích"];

  const handleNext = async () => {
    let valid = false;
    if (currentStep === 1) {
      valid = await form.trigger(["name", "address", "phone"]);
    } else if (currentStep === 2) {
      if (uploadedPhotos.length < 1) {
        toast.error("Vui lòng tải lên ít nhất 1 ảnh");
        return;
      }
      valid = true;
    }
    if (valid) setCurrentStep(prev => prev + 1);
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">Đăng ký Tổ hợp mới (L1)</h1>

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

        {/* Form Container */}
        <div>
          {/* Step 1: Basic Info */}
          {currentStep === 1 && (
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold">Thông tin cơ bản</h2>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Tên tổ hợp *</Label>
                  <Input
                    {...form.register("name")}
                    id="name"
                    placeholder="VD: Tổ hợp thể thao Kỳ Hòa"
                  />
                  {form.formState.errors.name && (
                    <p className="text-sm text-destructive">{form.formState.errors.name.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="phone">Số điện thoại liên hệ</Label>
                  <Input
                    {...form.register("phone")}
                    id="phone"
                    placeholder="VD: 0901234567"
                  />
                  {form.formState.errors.phone && (
                    <p className="text-sm text-destructive">{form.formState.errors.phone.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <AddressPicker
                    initialAddress={form.watch("address")}
                    initialLat={form.watch("latitude")}
                    initialLng={form.watch("longitude")}
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
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Mô tả</Label>
                  <Textarea
                    {...form.register("description")}
                    id="description"
                    placeholder="Mô tả chi tiết về tổ hợp, các tiện ích, quy định..."
                    rows={4}
                  />
                  {form.formState.errors.description && (
                    <p className="text-sm text-destructive">{form.formState.errors.description.message}</p>
                  )}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Step 2: Photos */}
          {currentStep === 2 && (
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold">Hình ảnh tổ hợp</h2>
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
                        <Badge className="absolute top-2 left-2">Ảnh bìa</Badge>
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
                  * Tải lên tối thiểu 1 ảnh. Ảnh đầu tiên sẽ làm ảnh bìa đại diện.
                </p>
              </CardContent>
            </Card>
          )}

          {/* Step 3: Sport Types & Amenities */}
          {currentStep === 3 && (
            <Card>
              <CardHeader>
                <h2 className="text-xl font-semibold">Môn thể thao & Tiện ích</h2>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="space-y-4">
                  <Label className="text-base font-bold text-slate-800">Các môn thể thao hỗ trợ *</Label>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                    {sportTypes.map((type) => {
                      const isChecked = form.watch("sportTypeIds").includes(type.sportTypeId);
                      return (
                        <div key={type.sportTypeId} className="flex items-center space-x-2.5 border rounded-lg p-3 bg-white hover:bg-slate-50 transition-colors">
                          <Checkbox
                            id={`sport-${type.sportTypeId}`}
                            checked={isChecked}
                            onCheckedChange={(checked) => {
                              const currentIds = form.getValues("sportTypeIds") || [];
                              if (checked) {
                                form.setValue("sportTypeIds", [...currentIds, type.sportTypeId], { shouldValidate: true });
                              } else {
                                form.setValue("sportTypeIds", currentIds.filter(id => id !== type.sportTypeId), { shouldValidate: true });
                              }
                            }}
                          />
                          <Label htmlFor={`sport-${type.sportTypeId}`} className="font-semibold text-slate-700 cursor-pointer select-none">
                            {type.sportName}
                          </Label>
                        </div>
                      );
                    })}
                  </div>
                  {form.formState.errors.sportTypeIds && (
                    <p className="text-sm text-destructive">{form.formState.errors.sportTypeIds.message}</p>
                  )}
                </div>

                <div className="space-y-4">
                  <Label className="text-base font-bold text-slate-800">Tiện ích sẵn có (Amenities)</Label>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                    {amenities.map((amenity) => {
                      const isChecked = form.watch("amenityIds").includes(amenity.amenityId);
                      return (
                        <div key={amenity.amenityId} className="flex items-center space-x-2.5 border rounded-lg p-3 bg-white hover:bg-slate-50 transition-colors">
                          <Checkbox
                            id={`amenity-${amenity.amenityId}`}
                            checked={isChecked}
                            onCheckedChange={(checked) => {
                              const currentIds = form.getValues("amenityIds") || [];
                              if (checked) {
                                form.setValue("amenityIds", [...currentIds, amenity.amenityId], { shouldValidate: true });
                              } else {
                                form.setValue("amenityIds", currentIds.filter(id => id !== amenity.amenityId), { shouldValidate: true });
                              }
                            }}
                          />
                          <Label htmlFor={`amenity-${amenity.amenityId}`} className="font-semibold text-slate-700 cursor-pointer select-none">
                            {amenity.name}
                          </Label>
                        </div>
                      );
                    })}
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
                Hoàn tất & Đăng ký
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default AddVenuePage;
