'use client'

import { useState, useEffect } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Plus, Edit, Trash2, Trophy, Loader2 } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { fetchSportTypes, createSportType, type SportType, type CreateSportTypeRequest } from "@/lib/api/sport-category";
import { useRouter } from "next/navigation";

const categorySchema = z.object({
  sportName: z.string().min(1, "Tên tiếng Việt là bắt buộc").max(50, "Tối đa 50 ký tự"),
  nameEn: z.string().max(50, "Tối đa 50 ký tự").optional(),
  sportCode: z.string().min(1, "Mã môn thể thao là bắt buộc").max(20, "Tối đa 20 ký tự"),
  icon: z.string().max(10, "Tối đa 10 ký tự").optional(),
  description: z.string().optional(),
});

type CategoryFormValues = z.infer<typeof categorySchema>;

function SportCategoriesPage() {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [categories, setCategories] = useState<SportType[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const loadCategories = async () => {
    try {
      setLoading(true);
      const data = await fetchSportTypes();
      setCategories(data);
    } catch (error: any) {
      toast.error(error.message || "Không thể tải danh sách môn thể thao");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCategories();
  }, []);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: {
      sportName: "",
      nameEn: "",
      sportCode: "",
      icon: "",
      description: "",
    },
  });

  const onSubmit = async (values: CategoryFormValues) => {
    try {
      setSubmitting(true);
      await createSportType(values as CreateSportTypeRequest);
      toast.success("Thêm loại môn thể thao thành công");
      setShowCreateDialog(false);
      reset();
      loadCategories();
    } catch (error: any) {
      toast.error(error.message || "Có lỗi xảy ra khi thêm môn thể thao");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold">Quản lý môn thể thao</h1>
          <Button onClick={() => {
            reset();
            setShowCreateDialog(true);
          }}>
            <Plus className="mr-2 h-5 w-5" />
            Thêm môn mới
          </Button>
        </div>

        {loading ? (
          <div className="flex flex-col items-center justify-center py-20">
            <Loader2 className="h-10 w-10 animate-spin text-primary mb-4" />
            <p className="text-muted-foreground">Đang tải danh sách...</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {categories.map((category) => (
              <Card key={category.sportTypeId} className={!category.isActive ? "opacity-60" : ""}>
                <CardContent className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className="text-4xl">{category.icon || "🏆"}</div>
                      <div>
                        <h3 className="text-lg font-semibold mb-0">{category.sportName}</h3>
                        <p className="text-xs font-mono text-muted-foreground uppercase">
                          {category.sportCode}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {category.nameEn}
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled // Edit feature not implemented in UC-ADM-07
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>

                  <p className="text-sm text-muted-foreground mb-4 line-clamp-2 min-h-[2.5rem]">
                    {category.description || "Chưa có mô tả."}
                  </p>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Trophy className="h-4 w-4" />
                      <span>{new Date(category.createdAt).toLocaleDateString('vi-VN')}</span>
                    </div>
                    <div
                      className={`text-xs px-2 py-1 rounded font-medium ${
                        category.isActive
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-700"
                      }`}
                    >
                      {category.isActive ? "Hoạt động" : "Tạm ngưng"}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
            {categories.length === 0 && (
              <div className="col-span-full text-center py-20 bg-muted/20 rounded-xl border-2 border-dashed">
                <p className="text-muted-foreground">Chưa có môn thể thao nào.</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Create Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Thêm môn thể thao mới</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="sportName">Tên tiếng Việt *</Label>
                <Input
                  id="sportName"
                  {...register("sportName")}
                  placeholder="VD: Bóng đá"
                />
                {errors.sportName && (
                  <p className="text-xs text-destructive">{errors.sportName.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="nameEn">Tên tiếng Anh</Label>
                <Input
                  id="nameEn"
                  {...register("nameEn")}
                  placeholder="VD: Football"
                />
                {errors.nameEn && (
                  <p className="text-xs text-destructive">{errors.nameEn.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="sportCode">Mã môn thể thao *</Label>
                <Input
                  id="sportCode"
                  {...register("sportCode")}
                  placeholder="VD: FOOTBALL"
                />
                {errors.sportCode && (
                  <p className="text-xs text-destructive">{errors.sportCode.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="icon">Icon (Emoji)</Label>
                <Input
                  id="icon"
                  {...register("icon")}
                  placeholder="VD: ⚽"
                  maxLength={2}
                />
                {errors.icon && (
                  <p className="text-xs text-destructive">{errors.icon.message}</p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Mô tả</Label>
              <Textarea
                id="description"
                {...register("description")}
                placeholder="Mô tả chi tiết về môn thể thao..."
                rows={3}
              />
            </div>

            <div className="flex gap-3 pt-4">
              <Button type="button" variant="outline" className="flex-1" onClick={() => setShowCreateDialog(false)}>
                Hủy
              </Button>
              <Button type="submit" className="flex-1" disabled={submitting}>
                {submitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  "Thêm mới"
                )}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default SportCategoriesPage;
