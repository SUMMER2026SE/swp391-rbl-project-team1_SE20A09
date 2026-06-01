'use client'

import * as React from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { toast } from 'sonner'
import { Plus, Loader2, Package, Boxes, Coins, CheckCircle, XCircle } from 'lucide-react'
import { get, post } from '@/lib/api'
import { Accessory } from '@/types/accessory'

interface AccessoryManagerDialogProps {
  stadiumId: number
  stadiumName: string
  isOpen: boolean
  onClose: () => void
}

export function AccessoryManagerDialog({
  stadiumId,
  stadiumName,
  isOpen,
  onClose,
}: AccessoryManagerDialogProps) {
  const [accessories, setAccessories] = React.useState<Accessory[]>([])
  const [loading, setLoading] = React.useState<boolean>(false)
  const [submitting, setSubmitting] = React.useState<boolean>(false)

  // Form states
  const [name, setName] = React.useState<string>('')
  const [pricePerUnit, setPricePerUnit] = React.useState<string>('')
  const [quantity, setQuantity] = React.useState<string>('')
  const [isAvailable, setIsAvailable] = React.useState<boolean>(true)

  const fetchAccessories = React.useCallback(async () => {
    setLoading(true)
    try {
      // Gọi real API từ Backend
      const data = await get<Accessory[]>(`/stadiums/${stadiumId}/accessories`)
      setAccessories(data)
    } catch (error: any) {
      console.error('Failed to fetch accessories:', error)
      toast.error('Không thể tải danh sách phụ kiện!')
      setAccessories([])
    } finally {
      setLoading(false)
    }
  }, [stadiumId])

  React.useEffect(() => {
    if (isOpen && stadiumId) {
      fetchAccessories()
      // Reset form
      setName('')
      setPricePerUnit('')
      setQuantity('')
      setIsAvailable(true)
    }
  }, [isOpen, stadiumId, fetchAccessories])

  const handleAddAccessory = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!name.trim()) {
      toast.error('Vui lòng nhập tên phụ kiện')
      return
    }
    const parsedPrice = parseFloat(pricePerUnit)
    if (isNaN(parsedPrice) || parsedPrice < 0) {
      toast.error('Giá thuê không hợp lệ (phải từ 0đ trở lên)')
      return
    }
    const parsedQty = parseInt(quantity)
    if (isNaN(parsedQty) || parsedQty < 0) {
      toast.error('Số lượng không hợp lệ (phải từ 0 trở lên)')
      return
    }

    setSubmitting(true)
    try {
      // Gửi request POST tới real Backend API
      const newAcc = await post<Accessory>(`/stadiums/${stadiumId}/accessories`, {
        name: name.trim(),
        pricePerUnit: parsedPrice,
        quantity: parsedQty,
        isAvailable,
      })

      setAccessories((prev) => [...prev, newAcc])
      toast.success(`Đã thêm phụ kiện "${name}" thành công!`)
      
      // Reset form
      setName('')
      setPricePerUnit('')
      setQuantity('')
      setIsAvailable(true)
    } catch (error: any) {
      console.error('Failed to add accessory:', error)
      const errorMsg = error?.message || 'Có lỗi xảy ra khi thêm phụ kiện!'
      toast.error(errorMsg)
    } finally {
      setSubmitting(false)
    }
  }

  const formatVND = (value: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(value)
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-4xl w-full bg-background/95 backdrop-blur-md border border-border/80 shadow-2xl p-6 sm:p-8 sm:rounded-xl">
        <DialogHeader className="mb-6">
          <DialogTitle className="flex items-center gap-2 text-2xl font-bold tracking-tight text-foreground">
            <Package className="h-7 w-7 text-primary animate-pulse" />
            Quản lý phụ kiện cho thuê
          </DialogTitle>
          <DialogDescription className="text-muted-foreground text-sm mt-1">
            Sân đang cấu hình: <span className="font-semibold text-foreground">{stadiumName}</span> (ID: {stadiumId})
          </DialogDescription>
        </DialogHeader>

        {/* Cấu trúc chia 2 phần: Danh sách & Thêm mới */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-8 mt-2">
          
          {/* Phần bên trái: Danh sách phụ kiện hiện có */}
          <div className="md:col-span-7 flex flex-col space-y-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <Boxes className="h-4 w-4 text-primary" /> Danh sách phụ kiện hiện có ({accessories.length})
            </h3>
            
            <div className="border border-border/50 rounded-lg overflow-hidden bg-card max-h-[380px] overflow-y-auto shadow-xs">
              {loading ? (
                <div className="flex flex-col items-center justify-center py-20 text-muted-foreground gap-2">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                  <p className="text-xs">Đang tải danh sách phụ kiện...</p>
                </div>
              ) : accessories.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-24 text-muted-foreground text-center px-4">
                  <Package className="h-12 w-12 text-muted-foreground/35 mb-2" />
                  <p className="text-sm font-medium">Chưa có phụ kiện nào được thêm</p>
                  <p className="text-xs text-muted-foreground/80 mt-1">Sử dụng biểu mẫu bên phải để thêm phụ kiện cho thuê đầu tiên của bạn.</p>
                </div>
              ) : (
                <Table className="w-full table-fixed">
                  <TableHeader className="bg-muted/50 sticky top-0 z-10">
                    <TableRow>
                      <TableHead className="text-xs font-bold text-muted-foreground py-3 pl-4 w-[40%]">Tên phụ kiện</TableHead>
                      <TableHead className="text-xs font-bold text-muted-foreground text-right py-3 w-[25%]">Giá thuê</TableHead>
                      <TableHead className="text-xs font-bold text-muted-foreground text-center py-3 w-[15%]">SL</TableHead>
                      <TableHead className="text-xs font-bold text-muted-foreground text-center py-3 pr-4 w-[20%]">Trạng thái</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {accessories.map((item) => (
                      <TableRow key={item.accessoryId} className="hover:bg-muted/40 transition-colors border-b border-border/40">
                        <TableCell className="font-semibold text-sm py-4 pl-4 text-foreground break-words whitespace-normal">
                          {item.name}
                        </TableCell>
                        <TableCell className="text-right text-sm text-primary font-medium py-4">
                          {formatVND(item.pricePerUnit)}
                        </TableCell>
                        <TableCell className="text-center text-sm py-4 text-foreground">
                          {item.quantity}
                        </TableCell>
                        <TableCell className="text-center py-4 pr-4">
                          {item.isAvailable ? (
                            <Badge variant="outline" className="bg-emerald-500/10 text-emerald-600 border-emerald-500/20 text-[10px] font-semibold px-2 py-0.5">
                              Đang thuê
                            </Badge>
                          ) : (
                            <Badge variant="outline" className="bg-destructive/10 text-destructive border-destructive/20 text-[10px] font-semibold px-2 py-0.5">
                              Tạm ngưng
                            </Badge>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </div>
          </div>

          {/* Phần bên phải: Form thêm phụ kiện mới */}
          <div className="md:col-span-5 border-t md:border-t-0 md:border-l border-border/60 pt-6 md:pt-0 md:pl-6">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5 mb-4">
              <Plus className="h-4 w-4 text-primary" /> Thêm phụ kiện mới
            </h3>

            <form onSubmit={handleAddAccessory} className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="acc-name" className="text-xs font-medium">Tên phụ kiện <span className="text-destructive">*</span></Label>
                <div className="relative">
                  <Input
                    id="acc-name"
                    placeholder="Ví dụ: Bóng Molten, Áo lưới..."
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="pr-8 bg-muted/20"
                    required
                  />
                  <Package className="absolute right-2.5 top-2.5 h-4 w-4 text-muted-foreground/60" />
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="acc-price" className="text-xs font-medium">Giá thuê (VND/Lượt) <span className="text-destructive">*</span></Label>
                <div className="relative">
                  <Input
                    id="acc-price"
                    type="number"
                    min="0"
                    placeholder="Ví dụ: 30000"
                    value={pricePerUnit}
                    onChange={(e) => setPricePerUnit(e.target.value)}
                    className="pr-8 bg-muted/20"
                    required
                  />
                  <Coins className="absolute right-2.5 top-2.5 h-4 w-4 text-muted-foreground/60" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label htmlFor="acc-qty" className="text-xs font-medium">Số lượng <span className="text-destructive">*</span></Label>
                  <div className="relative">
                    <Input
                      id="acc-qty"
                      type="number"
                      min="0"
                      placeholder="10"
                      value={quantity}
                      onChange={(e) => setQuantity(e.target.value)}
                      className="pr-8 bg-muted/20"
                      required
                    />
                    <Boxes className="absolute right-2.5 top-2.5 h-4 w-4 text-muted-foreground/60" />
                  </div>
                </div>

                <div className="flex flex-col space-y-2 justify-end pb-1.5">
                  <Label htmlFor="acc-available" className="text-xs font-medium cursor-pointer">Cho thuê ngay</Label>
                  <div className="flex items-center space-x-2 h-10">
                    <Switch
                      id="acc-available"
                      checked={isAvailable}
                      onCheckedChange={setIsAvailable}
                    />
                    <span className="text-xs text-muted-foreground">
                      {isAvailable ? 'Có' : 'Không'}
                    </span>
                  </div>
                </div>
              </div>

              <Button type="submit" disabled={submitting} className="w-full mt-4 flex items-center justify-center gap-1">
                {submitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  <>
                    <Plus className="h-4 w-4" />
                    Thêm phụ kiện
                  </>
                )}
              </Button>
            </form>
          </div>

        </div>
      </DialogContent>
    </Dialog>
  )
}
