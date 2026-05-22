'use client'

import { useState } from "react";
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Plus, Edit, Trash2, Trophy } from "lucide-react";

function SportCategoriesPage() {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [editingCategory, setEditingCategory] = useState<any>(null);
  const [deleteId, setDeleteId] = useState<number | null>(null);

  const [categories, setCategories] = useState([
    {
      id: 1,
      name: "Bóng đá",
      nameEn: "Football",
      description: "Sân bóng đá 5 người, 7 người, 11 người",
      icon: "⚽",
      venueCount: 245,
      active: true,
    },
    {
      id: 2,
      name: "Cầu lông",
      nameEn: "Badminton",
      description: "Sân cầu lông trong nhà và ngoài trời",
      icon: "🏸",
      venueCount: 156,
      active: true,
    },
    {
      id: 3,
      name: "Quần vợt",
      nameEn: "Tennis",
      description: "Sân quần vợt tiêu chuẩn",
      icon: "🎾",
      venueCount: 89,
      active: true,
    },
    {
      id: 4,
      name: "Bóng rổ",
      nameEn: "Basketball",
      description: "Sân bóng rổ ngoài trời và trong nhà",
      icon: "🏀",
      venueCount: 67,
      active: true,
    },
    {
      id: 5,
      name: "Bóng chuyền",
      nameEn: "Volleyball",
      description: "Sân bóng chuyền bãi biển và trong nhà",
      icon: "🏐",
      venueCount: 34,
      active: false,
    },
  ]);

  const handleDelete = (id: number) => {
    setCategories(categories.filter((c) => c.id !== id));
    setDeleteId(null);
  };

  const CategoryForm = ({
    category,
    onClose,
  }: {
    category?: any;
    onClose: () => void;
  }) => (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="name">Tên tiếng Việt *</Label>
          <Input
            id="name"
            defaultValue={category?.name}
            placeholder="VD: Bóng đá"
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="nameEn">Tên tiếng Anh *</Label>
          <Input
            id="nameEn"
            defaultValue={category?.nameEn}
            placeholder="VD: Football"
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="icon">Icon (Emoji) *</Label>
        <Input
          id="icon"
          defaultValue={category?.icon}
          placeholder="VD: ⚽"
          maxLength={2}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Mô tả</Label>
        <Textarea
          id="description"
          defaultValue={category?.description}
          placeholder="Mô tả chi tiết về môn thể thao..."
          rows={3}
        />
      </div>

      <div className="flex gap-2">
        <Button variant="outline" className="flex-1" onClick={onClose}>
          Hủy
        </Button>
        <Button className="flex-1" onClick={onClose}>
          {category ? "Cập nhật" : "Thêm mới"}
        </Button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl">Quản lý môn thể thao</h1>
          <Button onClick={() => setShowCreateDialog(true)}>
            <Plus className="mr-2 h-5 w-5" />
            Thêm môn mới
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {categories.map((category) => (
            <Card key={category.id}>
              <CardContent className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="text-4xl">{category.icon}</div>
                    <div>
                      <h3 className="mb-1">{category.name}</h3>
                      <p className="text-sm text-muted-foreground">
                        {category.nameEn}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setEditingCategory(category)}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeleteId(category.id)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </div>

                <p className="text-sm text-muted-foreground mb-4">
                  {category.description}
                </p>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Trophy className="h-4 w-4" />
                    <span>{category.venueCount} sân</span>
                  </div>
                  <div
                    className={`text-xs px-2 py-1 rounded ${
                      category.active
                        ? "bg-green-100 text-green-700"
                        : "bg-gray-100 text-gray-700"
                    }`}
                  >
                    {category.active ? "Hoạt động" : "Tạm ngưng"}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>

      {/* Create Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Thêm môn thể thao mới</DialogTitle>
          </DialogHeader>
          <CategoryForm onClose={() => setShowCreateDialog(false)} />
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog
        open={!!editingCategory}
        onOpenChange={() => setEditingCategory(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Chỉnh sửa môn thể thao</DialogTitle>
          </DialogHeader>
          <CategoryForm
            category={editingCategory}
            onClose={() => setEditingCategory(null)}
          />
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xác nhận xóa</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn xóa môn thể thao này? Hành động này không thể
              hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => deleteId && handleDelete(deleteId)}
              className="bg-destructive text-destructive-foreground"
            >
              Xóa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

export default SportCategoriesPage;
