'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { AddressPicker } from '@/components/AddressPicker'
import { toast } from 'sonner'
import { Loader2 } from 'lucide-react'

const bookingSchema = z.object({
  customerName: z.string().min(1, 'Tên không được để trống'),
  phoneNumber: z.string().regex(/^\d{10}$/, 'Số điện thoại không hợp lệ'),
  addressText: z.string().min(1, 'Địa chỉ không được để trống'),
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
})

type BookingFormValues = z.infer<typeof bookingSchema>

export default function BookingDemoPage() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const form = useForm<BookingFormValues>({
    resolver: zodResolver(bookingSchema),
    defaultValues: {
      customerName: '',
      phoneNumber: '',
      addressText: '',
      latitude: 10.8231,
      longitude: 106.6297,
    },
  })

  const onSubmit = async (data: BookingFormValues) => {
    setIsSubmitting(true)
    try {
      const res = await fetch('http://localhost:8080/api/v1/bookings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) {
        const error = await res.json()
        throw new Error(error.message || 'Đặt sân thất bại')
      }
      toast.success('Đặt sân thành công!')
      form.reset()
    } catch (error: any) {
      toast.error(error.message || 'Đã xảy ra lỗi')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">Đặt sân (Demo Address Picker)</h1>
        <form onSubmit={form.handleSubmit(onSubmit)}>
          <Card>
            <CardHeader>
              <h2 className="text-xl font-semibold">Thông tin đặt sân</h2>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="customerName">Tên khách hàng *</Label>
                <Input {...form.register('customerName')} id="customerName" placeholder="Nhập tên" />
                {form.formState.errors.customerName && (
                  <p className="text-sm text-destructive">{form.formState.errors.customerName.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="phoneNumber">Số điện thoại *</Label>
                <Input {...form.register('phoneNumber')} id="phoneNumber" placeholder="0xxxxxxxxx" />
                {form.formState.errors.phoneNumber && (
                  <p className="text-sm text-destructive">{form.formState.errors.phoneNumber.message}</p>
                )}
              </div>

              <AddressPicker
                onAddressChange={(data) => {
                  form.setValue('addressText', data.addressText, { shouldValidate: true })
                  form.setValue('latitude', data.lat, { shouldValidate: true })
                  form.setValue('longitude', data.lng, { shouldValidate: true })
                }}
              />
              {form.formState.errors.addressText && (
                <p className="text-sm text-destructive">{form.formState.errors.addressText.message}</p>
              )}

              <input type="hidden" {...form.register('latitude', { valueAsNumber: true })} />
              <input type="hidden" {...form.register('longitude', { valueAsNumber: true })} />

              <div className="flex justify-end pt-4">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Đang xử lý...
                    </>
                  ) : (
                    'Đặt sân'
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </form>
      </div>
    </div>
  )
}
