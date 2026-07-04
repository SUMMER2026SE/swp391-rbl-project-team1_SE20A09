import React, { useEffect, useRef } from 'react';

interface MessageScrollerProps {
  children: React.ReactNode;
  dependencies: any[];
}

export function MessageScroller({ children, dependencies }: MessageScrollerProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Cuộn xuống cuối cùng mỗi khi messages thay đổi
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);

  return (
    <div
      ref={containerRef}
      className="flex-1 overflow-y-auto px-4 py-3 min-h-0"
      style={{ scrollbarWidth: 'thin', scrollbarColor: 'hsl(var(--border)) transparent' }}
    >
      {children}
      {/* Anchor element để scrollIntoView luôn cuộn đến đây */}
      <div ref={bottomRef} />
    </div>
  );
}
