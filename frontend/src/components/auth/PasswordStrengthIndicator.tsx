'use client'

import { Check, X } from "lucide-react";
import { cn } from "@/lib/utils";

interface PasswordStrengthIndicatorProps {
  password?: string;
}

export function PasswordStrengthIndicator({ password = "" }: PasswordStrengthIndicatorProps) {
  const conditions = [
    { label: "Tối thiểu 8 ký tự", met: password.length >= 8 },
    { label: "Có ít nhất 1 chữ hoa (A-Z)", met: /[A-Z]/.test(password) },
    { label: "Có ít nhất 1 chữ số (0-9)", met: /[0-9]/.test(password) },
    { label: "Có ít nhất 1 ký tự đặc biệt (!@#$...)", met: /[^A-Za-z0-9]/.test(password) },
  ];

  const metCount = conditions.filter(c => c.met).length;

  const getStrengthColor = () => {
    if (metCount <= 1) return "bg-red-500";
    if (metCount === 2) return "bg-orange-500";
    if (metCount === 3) return "bg-yellow-500";
    return "bg-green-500";
  };

  return (
    <div className="space-y-3 mt-2 animate-in fade-in slide-in-from-top-1 duration-200">
      <div className="grid grid-cols-2 gap-2 text-xs">
        {conditions.map((condition, index) => (
          <div
            key={index}
            className={cn(
              "flex items-center space-x-1.5 transition-colors duration-200",
              condition.met ? "text-green-600" : "text-muted-foreground"
            )}
          >
            {condition.met ? (
              <Check className="h-3.5 w-3.5 stroke-[3]" />
            ) : (
              <div className="h-1 w-1 rounded-full bg-muted-foreground/40 ml-1.5 mr-1" />
            )}
            <span>{condition.label}</span>
          </div>
        ))}
      </div>

      <div className="flex h-1.5 w-full gap-1">
        {[1, 2, 3, 4].map((step) => (
          <div
            key={step}
            className={cn(
              "h-full flex-1 rounded-full transition-all duration-300",
              step <= metCount ? getStrengthColor() : "bg-muted"
            )}
          />
        ))}
      </div>
    </div>
  );
}
