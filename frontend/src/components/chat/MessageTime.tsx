import React from 'react';
import { useRelativeTime } from '@/hooks/useRelativeTime';

interface MessageTimeProps {
  timestamp: string | Date | null;
  status: 'sent' | 'seen';
}

export function MessageTime({ timestamp, status }: MessageTimeProps) {
  const relativeText = useRelativeTime(timestamp);
  
  if (!timestamp || relativeText === '—') return null;
  
  const prefix = status === 'seen' ? 'Đã xem' : 'Đã gửi';
  
  return (
    <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground mt-0 mb-1 pr-1 justify-end animate-in fade-in slide-in-from-top-1">
      <span>
        {prefix} {relativeText}
      </span>
    </div>
  );
}
