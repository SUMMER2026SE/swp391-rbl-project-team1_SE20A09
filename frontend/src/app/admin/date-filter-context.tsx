"use client";

import { createContext, useContext } from "react";

export interface DateRange {
  startDate: string; // yyyy-MM-dd
  endDate: string;   // yyyy-MM-dd
}

export interface DateFilterContextValue {
  dateRange: DateRange | null;
  isFilterOpen: boolean;
  setIsFilterOpen: (v: boolean) => void;
  applyDateRange: (range: DateRange) => void;
}

export const DateFilterContext = createContext<DateFilterContextValue>({
  dateRange: null,
  isFilterOpen: false,
  setIsFilterOpen: () => {},
  applyDateRange: () => {},
});

export function useDateFilter() {
  return useContext(DateFilterContext);
}
