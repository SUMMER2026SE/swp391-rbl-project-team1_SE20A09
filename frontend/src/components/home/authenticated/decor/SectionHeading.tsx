"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

type SectionHeadingProps = {
  title: string;
  subtitle?: string;
  badge?: ReactNode;
  action?: ReactNode;
  className?: string;
  delayClass?: string;
};

export function SectionHeading({
  title,
  subtitle,
  badge,
  action,
  className,
  delayClass = "",
}: SectionHeadingProps) {
  return (
    <div
      className={cn(
        "mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between",
        "animate-fade-in-up",
        delayClass,
        className,
      )}
    >
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-shimmer text-2xl font-bold tracking-tight md:text-3xl">
            {title}
          </h2>
          {badge}
        </div>
        {subtitle && (
          <p className="mt-1.5 max-w-2xl text-sm text-muted-foreground md:text-base">
            {subtitle}
          </p>
        )}
      </div>
      {action}
    </div>
  );
}
