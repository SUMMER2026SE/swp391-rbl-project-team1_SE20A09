'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useSession } from 'next-auth/react'
import { ArrowLeft, Loader2, User } from 'lucide-react'
import { Header } from '@/components/layout/Header'
import { Footer } from '@/components/landing/Footer'
import { AvatarUploader } from '@/components/profile/AvatarUploader'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { get, put } from '@/lib/api'
import type { UpdateProfilePayload, UserProfile } from '@/types/user'

const PHONE_REGEX = /^(0[35789]\d{8}|\+84[35789]\d{8})$/

function EditProfilePage() {
  const router = useRouter()
  const { data: session, status, update } = useSession()

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [email, setEmail] = useState('')
  const [avatarReady, setAvatarReady] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.replace('/login?callbackUrl=/profile/edit')
      return
    }

    if (status !== 'authenticated') return

    const loadProfile = async () => {
      setIsLoading(true)
      setError(null)
      try {
        const profile = await get<UserProfile>('/auth/me')
        setFirstName(profile.firstName)
        setLastName(profile.lastName ?? '')
        setPhoneNumber(profile.phoneNumber)
        setAvatarUrl(profile.avatarUrl ?? '')
        setEmail(profile.email)
        setAvatarReady(true)
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể tải hồ sơ.')
      } finally {
        setIsLoading(false)
      }
    }

    loadProfile()
  }, [status, router])

  const getInitials = () => {
    const f = firstName.trim().charAt(0)
    const l = lastName.trim().charAt(0)
    return (f + l).toUpperCase() || 'U'
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!avatarReady) {
      setError('Vui lòng hoàn tất tải và xác nhận ảnh đại diện trước khi lưu.')
      return
    }

    if (!firstName.trim()) {
      setError('Tên không được để trống.')
      return
    }

    if (!PHONE_REGEX.test(phoneNumber.trim())) {
      setError('Số điện thoại không đúng định dạng (ví dụ: 0901234567).')
      return
    }

    const payload: UpdateProfilePayload = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phoneNumber: phoneNumber.trim(),
      avatarUrl: avatarUrl.trim() || undefined,
    }

    setIsSaving(true)
    try {
      const updated = await put<UserProfile>('/auth/me', payload)
      await update({
        user: {
          ...session?.user,
          userId: updated.userId,
          email: updated.email,
          firstName: updated.firstName,
          lastName: updated.lastName,
          roleName: updated.roleName,
          avatarUrl: updated.avatarUrl ?? undefined,
          phoneNumber: updated.phoneNumber,
          userRank: updated.userRank,
          userPoint: updated.userPoint,
          accountStatus: updated.accountStatus,
        },
      })
      router.push('/profile')
      router.refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu hồ sơ.')
    } finally {
      setIsSaving(false)
    }
  }

  if (status === 'loading' || isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8 max-w-2xl">
        <Button variant="ghost" asChild className="mb-4">
          <Link href="/profile">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Quay lại hồ sơ
          </Link>
        </Button>

        <Card>
          <CardHeader>
            <h1 className="text-2xl font-semibold">Chỉnh sửa hồ sơ</h1>
            <p className="text-sm text-muted-foreground">
              Tải ảnh đại diện, sau đó cập nhật thông tin và nhấn Lưu
            </p>
          </CardHeader>

          <CardContent>
            {error && (
              <div className="mb-4 p-3 bg-red-100 dark:bg-red-950/50 text-red-600 dark:text-red-400 text-sm rounded-lg border border-red-200 dark:border-red-900/50">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              <AvatarUploader
                value={avatarUrl}
                onChange={setAvatarUrl}
                onConfirmedChange={setAvatarReady}
                disabled={isSaving}
                initials={getInitials()}
              />

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="firstName">Tên *</Label>
                  <Input
                    id="firstName"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    disabled={isSaving}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="lastName">Họ</Label>
                  <Input
                    id="lastName"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="phone">Số điện thoại *</Label>
                <Input
                  id="phone"
                  type="tel"
                  placeholder="0901234567"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  disabled={isSaving}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" type="email" value={email} disabled />
                <p className="text-xs text-muted-foreground">Email không thể thay đổi</p>
              </div>

              <div className="flex gap-3 pt-2">
                <Button type="submit" disabled={isSaving || !avatarReady}>
                  {isSaving ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Đang lưu...
                    </>
                  ) : (
                    <>
                      <User className="mr-2 h-4 w-4" />
                      Lưu thay đổi
                    </>
                  )}
                </Button>
                <Button type="button" variant="outline" asChild disabled={isSaving}>
                  <Link href="/profile">Hủy</Link>
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>

      <Footer />
    </div>
  )
}

export default EditProfilePage
