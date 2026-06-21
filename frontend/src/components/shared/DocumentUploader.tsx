'use client'

import { useRef, useState } from 'react'
import { Loader2, UploadCloud, X, ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { uploadDocument } from '@/lib/api'

interface DocumentUploaderProps {
  value?: string
  onChange: (url: string) => void
  disabled?: boolean
  label?: string
  accept?: string
}

export function DocumentUploader({
  value,
  onChange,
  disabled,
  label,
  accept = 'image/*,.jpg,.jpeg,.png,.webp'
}: DocumentUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    // Validate size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      setError('File không được vượt quá 5MB')
      return
    }

    setIsUploading(true)
    setError(null)
    try {
      const result = await uploadDocument(file)
      onChange(result.url)
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Tải file lên thất bại'
      setError(message)
    } finally {
      setIsUploading(false)
    }
  }

  const handleRemove = () => {
    onChange('')
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <div className="space-y-2">
      {label && <label className="text-sm font-medium text-foreground">{label}</label>}

      {value ? (
        /* ── Trạng thái: đã có ảnh — hiển thị preview lớn ── */
        <div className="relative border border-border rounded-xl overflow-hidden bg-muted/20 group">
          {/* Nút xoá */}
          {!disabled && (
            <Button
              type="button"
              variant="destructive"
              size="icon"
              className="absolute top-2 right-2 z-10 h-7 w-7 rounded-full shadow-md opacity-0 group-hover:opacity-100 transition-opacity"
              onClick={handleRemove}
            >
              <X className="h-4 w-4" />
            </Button>
          )}

          {/* Preview ảnh — dùng <img> thường để tránh lỗi domain whitelist next/image */}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={value}
            alt="Xem trước tài liệu"
            className="w-full object-contain max-h-52 bg-white"
          />

          {/* Footer: link xem gốc */}
          <div className="px-3 py-2 border-t border-border bg-muted/30 flex items-center justify-between gap-2">
            <p className="text-xs text-muted-foreground truncate flex-1">Tài liệu đã được tải lên</p>
            <a
              href={value}
              target="_blank"
              rel="noreferrer"
              className="text-xs text-primary font-semibold hover:underline flex items-center gap-1 shrink-0"
              onClick={(e) => e.stopPropagation()}
            >
              <ExternalLink className="h-3 w-3" />
              Xem ảnh gốc
            </a>
          </div>
        </div>
      ) : (
        /* ── Trạng thái: chưa upload ── */
        <div
          onClick={() => !disabled && !isUploading && fileInputRef.current?.click()}
          className={`border-2 border-dashed border-muted-foreground/25 rounded-xl p-8 text-center transition-colors ${
            disabled
              ? 'opacity-50 cursor-not-allowed'
              : 'cursor-pointer hover:border-teal-400 hover:bg-teal-50/30'
          }`}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept={accept}
            className="hidden"
            onChange={handleFileChange}
            disabled={disabled || isUploading}
          />
          {isUploading ? (
            <div className="flex flex-col items-center gap-2">
              <Loader2 className="h-9 w-9 animate-spin text-teal-600" />
              <p className="text-sm font-medium text-muted-foreground">Đang tải ảnh lên...</p>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-2">
              <div className="w-12 h-12 rounded-full bg-teal-50 flex items-center justify-center">
                <UploadCloud className="h-6 w-6 text-teal-600" />
              </div>
              <p className="text-sm font-semibold text-slate-700">Nhấn để tải ảnh lên</p>
              <p className="text-xs text-muted-foreground">JPG, PNG hoặc WEBP · Tối đa 5MB</p>
            </div>
          )}
        </div>
      )}

      {error && <p className="text-xs text-destructive font-medium">{error}</p>}
    </div>
  )
}
