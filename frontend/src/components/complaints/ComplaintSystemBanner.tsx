'use client'

import { AlertCircle } from 'lucide-react'

const SYSTEM_MESSAGE_PREFIXES = {
  proposal: 'Đã đề xuất giải pháp:',
  objection: 'Khách hàng phản đối:',
} as const

export type ComplaintSystemMessageType = keyof typeof SYSTEM_MESSAGE_PREFIXES

/** Trả về loại banner hệ thống nếu `message` khớp 1 trong 2 prefix đã biết,
 * ngược lại `null` — dùng ở trang gọi để quyết định render banner hay bubble
 * chat thường. Tách riêng khỏi component để trang gọi có thể check trước khi
 * quyết định key/layout mà không cần render thử. */
export function getComplaintSystemMessageType(message: string): ComplaintSystemMessageType | null {
  if (message.startsWith(SYSTEM_MESSAGE_PREFIXES.proposal)) return 'proposal'
  if (message.startsWith(SYSTEM_MESSAGE_PREFIXES.objection)) return 'objection'
  return null
}

// Class Tailwind literal đầy đủ theo từng variant — KHÔNG ghép chuỗi động
// (vd `bg-${color}-50`) vì JIT compiler của Tailwind chỉ nhận diện được class
// name xuất hiện nguyên văn trong source, chuỗi ghép động sẽ bị thiếu CSS lúc build.
const STYLES = {
  proposal: {
    wrapper: 'bg-orange-50 border border-orange-200 rounded-lg p-3 text-sm max-w-[85%] shadow-sm w-full',
    header: 'flex items-center text-orange-700 font-bold gap-1 mb-1',
    body: 'text-orange-800 whitespace-pre-wrap',
    time: 'text-[10px] text-orange-600/80 mt-1.5 font-mono text-right',
    defaultLabel: 'Đã đề xuất giải pháp',
  },
  objection: {
    wrapper: 'bg-purple-50 border border-purple-200 rounded-lg p-3 text-sm max-w-[85%] shadow-sm w-full',
    header: 'flex items-center text-purple-700 font-bold gap-1 mb-1',
    body: 'text-purple-800 whitespace-pre-wrap',
    time: 'text-[10px] text-purple-600/80 mt-1.5 font-mono text-right',
    defaultLabel: 'Khách hàng phản đối',
  },
} as const

interface ComplaintSystemBannerProps {
  message: string
  time: string
  /** Ghi đè label mặc định — vd trang Customer muốn ghi rõ "Chủ sân đề xuất
   * giải pháp" thay vì "Đã đề xuất giải pháp" trung tính ở trang Owner/Admin. */
  proposalLabel?: string
  objectionLabel?: string
}

/**
 * Banner cho tin nhắn hệ thống (đề xuất giải pháp / khách phản đối) trong
 * thread khiếu nại — dùng chung cho Customer/Owner/Admin thay vì lặp lại JSX
 * ở cả 3 trang. Trả về `null` nếu `message` không khớp loại nào, để trang gọi
 * tự render bubble chat thường thay thế.
 */
export function ComplaintSystemBanner({ message, time, proposalLabel, objectionLabel }: ComplaintSystemBannerProps) {
  const type = getComplaintSystemMessageType(message)
  if (!type) return null

  const style = STYLES[type]
  const prefix = SYSTEM_MESSAGE_PREFIXES[type]
  const label = type === 'proposal' ? (proposalLabel ?? style.defaultLabel) : (objectionLabel ?? style.defaultLabel)

  return (
    <div className="flex justify-center my-2 w-full">
      <div className={style.wrapper}>
        <div className={style.header}>
          <AlertCircle className="h-4 w-4" /> {label}
        </div>
        <p className={style.body}>{message.replace(`${prefix} `, '')}</p>
        <div className={style.time}>{time}</div>
      </div>
    </div>
  )
}
