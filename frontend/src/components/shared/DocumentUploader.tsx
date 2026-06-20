'use client'

import { useRef, useState } from 'react'
import { FileText, Loader2, UploadCloud, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { uploadDocument } from '@/lib/api'
import Image from 'next/image'

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
    } catch (err: any) {
      setError(err.message || 'Tải file lên thất bại')
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
        <div className="relative border border-border rounded-lg p-2 bg-muted/30 flex items-center gap-4 group">
          <div className="relative w-16 h-16 rounded overflow-hidden border border-border bg-white flex items-center justify-center shrink-0">
            <Image
              src={value}
              alt="Document preview"
              fill
              className="object-cover"
              sizes="64px"
            />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs text-muted-foreground truncate">Tài liệu đã được tải lên</p>
            <a
              href={value}
              target="_blank"
              rel="noreferrer"
              className="text-xs text-primary font-medium hover:underline truncate block"
            >
              Xem ảnh gốc
            </a>
          </div>
          {!disabled && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="absolute top-2 right-2 h-6 w-6 text-muted-foreground hover:text-destructive rounded-full"
              onClick={handleRemove}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      ) : (
        <div
          onClick={() => !disabled && !isUploading && fileInputRef.current?.click()}
          className={`border-2 border-dashed border-muted-foreground/20 rounded-lg p-6 text-center cursor-pointer hover:border-primary/50 transition-colors ${
            disabled ? 'opacity-50 cursor-not-allowed' : ''
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
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="text-sm font-medium text-muted-foreground">Đang tải tài liệu lên...</p>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-2">
              <UploadCloud className="h-8 w-8 text-muted-foreground group-hover:text-primary transition-colors" />
              <p className="text-sm font-semibold">Tải ảnh lên</p>
              <p className="text-xs text-muted-foreground">Nhấp để tải lên JPG, PNG hoặc WEBP (tối đa 5MB)</p>
            </div>
          )}
        </div>
      )}

      {error && <p className="text-xs text-destructive font-medium">{error}</p>}
    </div>
  )
}
