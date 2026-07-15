import { useState, useEffect, useRef } from "react";

export function useTypewriter(text: string, speed: number = 20) {
  const [displayText, setDisplayText] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const indexRef = useRef(0);
  const textRef = useRef(text);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    textRef.current = text;
    indexRef.current = 0;
    setDisplayText("");
    setIsTyping(text.length > 0);

    if (timerRef.current) {
      clearInterval(timerRef.current);
    }

    if (!text) {
      setIsTyping(false);
      return;
    }

    timerRef.current = setInterval(() => {
      if (indexRef.current < textRef.current.length) {
        setDisplayText((prev) => prev + textRef.current.charAt(indexRef.current));
        indexRef.current += 1;
      } else {
        setIsTyping(false);
        if (timerRef.current) {
          clearInterval(timerRef.current);
        }
      }
    }, speed);

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [text, speed]);

  return { displayText, isTyping };
}
