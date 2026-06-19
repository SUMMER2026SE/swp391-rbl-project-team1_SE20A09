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
import { Plus, Trash2, Trophy, Loader2, X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { fetchSportTypes, createSportType, type SportType, type CreateSportTypeRequest } from "@/lib/api/sport-category";

const categorySchema = z.object({
  sportName: z.string()
    .min(1, "Tên môn thể thao là bắt buộc")
    .max(50, "Tối đa 50 ký tự")
    .regex(/^[^<>]*$/, "Không được chứa ký tự đặc biệt < hoặc >"),
  fieldTypes: z.array(z.string()),
  internalNote: z.string()
    .max(500, "Tối đa 500 ký tự")
    .regex(/^[^<>]*$/, "Không được chứa ký tự đặc biệt < hoặc >")
    .optional()
    .or(z.literal("")),
});

type CategoryFormValues = z.infer<typeof categorySchema>;

// Helper to format sport name to upper snake_case slug without Vietnamese tones
const removeVietnameseTones = (str: string) => {
  str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, "a");
  str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, "e");
  str = str.replace(/ì|í|ị|ỉ|ĩ/g, "i");
  str = str.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, "o");
  str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, "u");
  str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, "y");
  str = str.replace(/đ/g, "d");
  str = str.replace(/À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ/g, "A");
  str = str.replace(/È|É|Ẹ|Ẻ|Ẽ|Ê|Ề|Ế|Ệ|Ể|Ễ/g, "E");
  str = str.replace(/Ì|Í|Ị|Ỉ|Ĩ/g, "I");
  str = str.replace(/Ò|Ó|Ọ|Ỏ|Õ|Ô|Ồ|Ố|Ộ|Ổ|Ỗ|Ơ|Ờ|Ớ|Ợ|Ở|Ỡ/g, "O");
  str = str.replace(/Ù|Ú|Ụ|Ủ|U|Ư|Ừ|Ứ|Ự|Ử|Ữ/g, "U");
  str = str.replace(/Ỳ|Ý|Ỵ|Ỷ|Ỹ/g, "Y");
  str = str.replace(/Đ/g, "D");
  str = str.replace(/\u0300|\u0301|\u0303|\u0309|\u0323/g, ""); 
  str = str.replace(/\u02C6|\u0306|\u031B/g, ""); 
  return str;
};

const formatSportCode = (name: string) => {
  return removeVietnameseTones(name)
    .toUpperCase()
    .replace(/[^A-Z0-9\s]/g, "")
    .trim()
    .replace(/\s+/g, "_");
};

function SportCategoriesPage() {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [categories, setCategories] = useState<SportType[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [tagInput, setTagInput] = useState("");

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
    watch,
    setValue,
    formState: { errors },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: {
      sportName: "",
      fieldTypes: [],
      internalNote: "",
    },
  });

  const sportNameValue = watch("sportName");
  const fieldTypesValue = watch("fieldTypes") || [];

  const handleAddTag = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const trimmedValue = tagInput.trim();
      if (trimmedValue && !fieldTypesValue.includes(trimmedValue)) {
        setValue("fieldTypes", [...fieldTypesValue, trimmedValue], { shouldValidate: true });
        setTagInput("");
      }
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setValue(
      "fieldTypes",
      fieldTypesValue.filter((tag) => tag !== tagToRemove),
      { shouldValidate: true }
    );
  };

  const onSubmit = async (values: CategoryFormValues) => {
    if (submitting) return;
    try {
      setSubmitting(true);
      const code = formatSportCode(values.sportName);
      const payload: CreateSportTypeRequest = {
        sportName: values.sportName,
        sportCode: code,
        description: values.internalNote || undefined,
        isActive: true,
        isFootballType: code.startsWith("FOOTBALL"),
      };
      const newCategory = await createSportType(payload);
      toast.success("Thêm loại môn thể thao thành công");
      setShowCreateDialog(false);
      reset();
      setTagInput("");
      setCategories((prev) => [...prev, newCategory]);
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
          <h1 className="text-3xl font-bold">Quản lý danh mục thể thao</h1>
          <Button onClick={() => {
            reset();
            setTagInput("");
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
                      <div className="p-2 bg-primary/10 rounded-lg text-primary">
                        <Trophy className="h-6 w-6" />
                      </div>
                      <div>
                        <h3 className="text-lg font-semibold text-foreground leading-tight">{category.sportName}</h3>
                        <p className="text-xs font-mono text-muted-foreground/80 mt-1 uppercase">
                          {category.sportCode}
                        </p>
                      </div>
                    </div>
                    <div>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-destructive hover:bg-destructive/10 hover:text-destructive"
                        title="Xóa"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>

                  <p className={`text-sm mb-4 line-clamp-2 min-h-[2.5rem] ${
                    !category.description ? "text-muted-foreground/60 italic" : "text-muted-foreground"
                  }`}>
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
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 pt-4">
            <div className="space-y-2">
              <Label htmlFor="sportName">Tên môn thể thao *</Label>
              <Input
                id="sportName"
                {...register("sportName")}
                placeholder="VD: Bóng đá, Cầu lông"
                className="w-full"
              />
              {errors.sportName && (
                <p className="text-xs text-destructive">{errors.sportName.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="fieldTypes">Các loại sân / Biến thể (Tùy chọn)</Label>
              <div className="flex flex-wrap gap-2 p-2 border rounded-md min-h-[42px] bg-background focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2">
                {fieldTypesValue.map((tag) => (
                  <span
                    key={tag}
                    className="flex items-center gap-1 bg-primary/10 text-primary text-xs font-semibold px-2 py-1 rounded-full border border-primary/20 hover:bg-primary/20 transition-colors"
                  >
                    {tag}
                    <button
                      type="button"
                      onClick={() => handleRemoveTag(tag)}
                      className="text-primary hover:text-primary-foreground hover:bg-primary rounded-full p-0.5 transition-colors"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                ))}
                <input
                  type="text"
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={handleAddTag}
                  placeholder={fieldTypesValue.length === 0 ? "Gõ loại sân và nhấn Enter (VD: Sân 5, Sân 7)..." : "Thêm loại sân..."}
                  className="flex-1 bg-transparent border-0 outline-none p-0 text-sm focus:ring-0 placeholder:text-muted-foreground min-w-[150px]"
                />
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Thêm các loại sân (VD: Sân 5, Sân 7...) để Chủ sân chọn khi đăng ký cơ sở kinh doanh.
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="internalNote">Ghi chú nội bộ</Label>
              <Textarea
                id="internalNote"
                {...register("internalNote")}
                placeholder="Ghi chú thêm về môn thể thao..."
                rows={3}
              />
              <p className="text-xs text-muted-foreground">
                Ghi chú này chỉ hiển thị cho Admin quản lý, không hiển thị cho người dùng.
              </p>
              {errors.internalNote && (
                <p className="text-xs text-destructive">{errors.internalNote.message}</p>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => setShowCreateDialog(false)}>
                Hủy
              </Button>
              <Button type="submit" disabled={submitting}>
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

