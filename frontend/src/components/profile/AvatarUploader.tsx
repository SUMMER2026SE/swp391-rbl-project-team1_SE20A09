'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { useSession } from 'next-auth/react'
import {
  Camera,
  Check,
  FolderOpen,
  HardDrive,
  Loader2,
  X,
} from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { uploadAvatar } from '@/lib/api'

const MAX_FILE_SIZE = 5 * 1024 * 1024
const IMAGE_EXT_REGEX = /\.(jpe?g|png|gif|webp|bmp|heic|heif)$/i

function isImageFile(file: File): boolean {
  if (file.type.startsWith('image/')) return true
  return IMAGE_EXT_REGEX.test(file.name)
}

type PickerDoc = {
  id: string
  name: string
  mimeType: string
}

type AvatarUploaderProps = {
  value: string
  onChange: (url: string) => void
  onConfirmedChange?: (confirmed: boolean) => void
  disabled?: boolean
  initials: string
}

function loadScript(src: string, id: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.getElementById(id)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.id = id
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Không tải được script: ${src}`))
    document.body.appendChild(script)
  })
}

export function AvatarUploader({
  value,
  onChange,
  onConfirmedChange,
  disabled,
  initials,
}: AvatarUploaderProps) {
  const { data: session } = useSession()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [step, setStep] = useState<1 | 2 | 3>(1)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [uploadedUrl, setUploadedUrl] = useState<string | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [isConfirmed, setIsConfirmed] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pickerLoading, setPickerLoading] = useState(false)

  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
  const googleAccessToken = session?.googleAccessToken

  useEffect(() => {
    onConfirmedChange?.(isConfirmed)
  }, [isConfirmed, onConfirmedChange])

  const resetDialog = useCallback(() => {
    setStep(1)
    setPreviewUrl(null)
    setUploadedUrl(null)
    setError(null)
    setIsUploading(false)
    setPickerLoading(false)
  }, [])

  const openDialog = () => {
    if (disabled) return
    resetDialog()
    setDialogOpen(true)
  }

  const uploadFile = async (file: File) => {
    if (!isImageFile(file)) {
      setError('Vui lòng chọn file ảnh (JPG, PNG, WEBP, GIF). Ảnh HEIC nên đổi sang JPG nếu không tải được.')
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      setError('Ảnh không được vượt quá 5MB.')
      return
    }

    setError(null)
    setIsConfirmed(false)
    setIsUploading(true)
    setStep(2)

    const localPreview = URL.createObjectURL(file)
    setPreviewUrl(localPreview)

    try {
      const result = await uploadAvatar(file)
      setUploadedUrl(result.url)
      setPreviewUrl(result.url)
      setStep(3)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Tải ảnh thất bại.')
      setStep(1)
      URL.revokeObjectURL(localPreview)
      setPreviewUrl(null)
    } finally {
      setIsUploading(false)
    }
  }

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      uploadFile(file)
    }
    e.target.value = ''
  }

  const downloadDriveFile = async (fileId: string, fileName: string, mimeType: string) => {
    if (!googleAccessToken) {
      setError('Cần đăng nhập bằng Google để chọn ảnh từ Drive.')
      return
    }

    setPickerLoading(true)
    setError(null)

    try {
      const response = await fetch(
        `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`,
        { headers: { Authorization: `Bearer ${googleAccessToken}` } }
      )

      if (!response.ok) {
        throw new Error('Không thể tải ảnh từ Google Drive.')
      }

      const blob = await response.blob()
      const extension = mimeType.includes('png')
        ? '.png'
        : mimeType.includes('webp')
          ? '.webp'
          : mimeType.includes('gif')
            ? '.gif'
            : '.jpg'
      const safeName = fileName.replace(/[^\w.\-]/g, '_') || `drive-image${extension}`
      const file = new File([blob], safeName.endsWith(extension) ? safeName : safeName + extension, {
        type: mimeType.startsWith('image/') ? mimeType : 'image/jpeg',
      })

      await uploadFile(file)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lấy ảnh từ Google Drive.')
    } finally {
      setPickerLoading(false)
    }
  }

  const openGooglePicker = async () => {
    if (!googleClientId) {
      setError('Chưa cấu hình Google Client ID cho ứng dụng.')
      return
    }
    if (!googleAccessToken) {
      setError(
        'Vui lòng đăng nhập bằng Google (và cấp quyền Drive) để chọn ảnh từ Google Drive.'
      )
      return
    }

    setPickerLoading(true)
    setError(null)

    try {
      await loadScript('https://apis.google.com/js/api.js', 'google-api')
      await loadScript('https://accounts.google.com/gsi/client', 'google-gsi')

      await new Promise<void>((resolve) => {
        const gapi = (window as unknown as { gapi?: { load: (n: string, cb: () => void) => void } }).gapi
        if (!gapi) {
          throw new Error('Google API chưa sẵn sàng.')
        }
        gapi.load('picker', () => resolve())
      })

      const google = window as unknown as {
        google?: {
          picker: {
            ViewId: { DOCS_IMAGES: string }
            Action: { PICKED: string }
            Feature: { NAV_HIDDEN: string }
            PickerBuilder: new () => {
              addView: (view: unknown) => unknown
              setOAuthToken: (token: string) => unknown
              setCallback: (cb: (data: { action: string; docs: PickerDoc[] }) => void) => unknown
              enableFeature: (f: string) => unknown
              setTitle: (t: string) => unknown
              build: () => { setVisible: (v: boolean) => void }
            }
            DocsView: new (viewId: string) => unknown
          }
        }
      }

      if (!google.google?.picker) {
        throw new Error('Google Picker chưa sẵn sàng.')
      }

      const view = new google.google.picker.DocsView(google.google.picker.ViewId.DOCS_IMAGES)
      const picker = new google.google.picker.PickerBuilder()
        .addView(view)
        .setOAuthToken(googleAccessToken)
        .enableFeature(google.google.picker.Feature.NAV_HIDDEN)
        .setTitle('Chọn ảnh từ Google Drive')
        .setCallback((data) => {
          setPickerLoading(false)
          if (data.action === google.google!.picker.Action.PICKED && data.docs?.[0]) {
            const doc = data.docs[0]
            downloadDriveFile(doc.id, doc.name, doc.mimeType)
          }
        })
        .build()

      picker.setVisible(true)
    } catch (err) {
      setPickerLoading(false)
      setError(err instanceof Error ? err.message : 'Không mở được Google Drive.')
    }
  }

  const confirmAvatar = () => {
    if (!uploadedUrl) return
    onChange(uploadedUrl)
    setIsConfirmed(true)
    setDialogOpen(false)
    resetDialog()
  }

  const displayUrl = previewUrl || value || undefined

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-4">
        <Avatar className="h-24 w-24 border-2 border-primary/20">
          <AvatarImage src={displayUrl} alt="Ảnh đại diện" />
          <AvatarFallback className="bg-primary/10 text-primary font-bold text-lg">
            {initials}
          </AvatarFallback>
        </Avatar>

        <div className="flex-1 space-y-2">
          <p className="text-sm font-medium">Ảnh đại diện</p>
          <p className="text-xs text-muted-foreground">
            Tải ảnh từ máy tính, thư viện ảnh hoặc Google Drive
          </p>
          <Button type="button" variant="outline" size="sm" onClick={openDialog} disabled={disabled}>
            <Camera className="h-4 w-4 mr-2" />
            Tải ảnh đại diện
          </Button>
          {isConfirmed && value ? (
            <p className="text-xs text-green-600 flex items-center gap-1">
              <Check className="h-3 w-3" />
              Ảnh đã sẵn sàng — bạn có thể lưu hồ sơ
            </p>
          ) : (
            <p className="text-xs text-amber-600">
              Vui lòng tải và xác nhận ảnh trước khi lưu (hoặc giữ ảnh hiện tại)
            </p>
          )}
        </div>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*,.jpg,.jpeg,.png,.webp,.gif,.bmp,.heic,.heif"
        className="hidden"
        onChange={handleFileInput}
      />

      <Dialog
        open={dialogOpen}
        onOpenChange={(open) => {
          setDialogOpen(open)
          if (!open) {
            resetDialog()
            if (value) setIsConfirmed(true)
          }
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Tải ảnh đại diện</DialogTitle>
            <DialogDescription>
              {step === 1 && 'Bước 1: Chọn nguồn ảnh'}
              {step === 2 && 'Bước 2: Đang tải ảnh lên...'}
              {step === 3 && 'Bước 3: Xem trước và xác nhận'}
            </DialogDescription>
          </DialogHeader>

          {error && (
            <div className="p-3 text-sm text-red-600 bg-red-50 dark:bg-red-950/30 rounded-lg border border-red-200">
              {error}
            </div>
          )}

          {step === 1 && (
            <div className="grid gap-3 py-2">
              <Button
                type="button"
                variant="outline"
                className="h-auto py-4 flex flex-col items-center gap-2"
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploading || pickerLoading}
              >
                <HardDrive className="h-8 w-8 text-primary" />
                <span className="font-medium">Từ máy tính / thư viện ảnh</span>
                <span className="text-xs text-muted-foreground text-center">
                  Chọn file JPG, PNG, WEBP, GIF (tối đa 5MB)
                </span>
              </Button>

              <Button
                type="button"
                variant="outline"
                className="h-auto py-4 flex flex-col items-center gap-2"
                onClick={openGooglePicker}
                disabled={isUploading || pickerLoading}
              >
                {pickerLoading ? (
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                ) : (
                  <FolderOpen className="h-8 w-8 text-primary" />
                )}
                <span className="font-medium">Từ Google Drive</span>
                <span className="text-xs text-muted-foreground text-center">
                  Duyệt và chọn ảnh trong Drive của bạn
                </span>
              </Button>
            </div>
          )}

          {step === 2 && (
            <div className="flex flex-col items-center py-8 gap-3">
              <Loader2 className="h-10 w-10 animate-spin text-primary" />
              <p className="text-sm text-muted-foreground">Đang tải ảnh lên máy chủ...</p>
            </div>
          )}

          {step === 3 && uploadedUrl && (
            <div className="space-y-4 py-2">
              <div className="flex justify-center">
                <Avatar className="h-32 w-32 border-2 border-primary/30">
                  <AvatarImage src={previewUrl ?? uploadedUrl} />
                  <AvatarFallback>{initials}</AvatarFallback>
                </Avatar>
              </div>
              <p className="text-sm text-center text-muted-foreground">
                Bạn có muốn dùng ảnh này làm ảnh đại diện?
              </p>
              <div className="flex gap-2">
                <Button type="button" className="flex-1" onClick={confirmAvatar}>
                  <Check className="h-4 w-4 mr-2" />
                  Xác nhận ảnh
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setStep(1)
                    setPreviewUrl(null)
                    setUploadedUrl(null)
                  }}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
