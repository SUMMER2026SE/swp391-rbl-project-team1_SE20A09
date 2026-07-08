import { useState, useEffect, useCallback } from 'react';

export function useRelativeTime(timestamp: string | Date | null) {
  const [relativeText, setRelativeText] = useState('—');

  const updateTime = useCallback(() => {
    if (!timestamp) {
      setRelativeText('—');
      return;
    }

    const time = new Date(timestamp).getTime();
    const now = Date.now();
    const diffMs = now - time;
    
    if (diffMs < 0 || isNaN(time)) {
      setRelativeText('—');
      return;
    }

    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays >= 7) {
      const d = new Date(time);
      const dd = d.getDate().toString().padStart(2, '0');
      const mm = (d.getMonth() + 1).toString().padStart(2, '0');
      setRelativeText(`${dd}/${mm}/${d.getFullYear()}`);
    } else if (diffDays >= 1) {
      setRelativeText(`${diffDays} ngày trước`);
    } else if (diffHours >= 1) {
      setRelativeText(`${diffHours} giờ trước`);
    } else if (diffMins >= 1) {
      setRelativeText(`${diffMins} phút trước`);
    } else {
      setRelativeText('vừa xong');
    }
  }, [timestamp]);

  useEffect(() => {
    updateTime();
    if (!timestamp) return;

    const now = new Date();
    const msUntilNextMinute = 60000 - (now.getSeconds() * 1000 + now.getMilliseconds());
    
    let interval: NodeJS.Timeout;
    const timeout = setTimeout(() => {
      updateTime();
      interval = setInterval(updateTime, 60000);
    }, msUntilNextMinute);

    return () => {
      clearTimeout(timeout);
      if (interval) clearInterval(interval);
    };
  }, [timestamp, updateTime]);

  return relativeText;
}
