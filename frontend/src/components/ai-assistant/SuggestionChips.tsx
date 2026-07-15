import { Button } from "../ui/button";
import { Search, Calendar, HelpCircle, Users } from "lucide-react";

interface SuggestionChipsProps {
  onSelect: (text: string) => void;
}

export function SuggestionChips({ onSelect }: SuggestionChipsProps) {
  const suggestions = [
    { icon: <Search className="h-3.5 w-3.5" />, text: "Tìm sân bóng đá Thủ Đức" },
    { icon: <Calendar className="h-3.5 w-3.5" />, text: "Lịch trống sân cầu lông ngày mai" },
    { icon: <Users className="h-3.5 w-3.5" />, text: "Tìm kèo ghép hôm nay" },
    { icon: <HelpCircle className="h-3.5 w-3.5" />, text: "Chính sách hủy đặt sân" },
  ];

  return (
    <div className="flex flex-wrap gap-2 justify-center w-full max-w-lg mx-auto py-2">
      {suggestions.map((action, idx) => (
        <Button
          key={idx}
          variant="outline"
          size="sm"
          className="gap-1.5 text-xs text-gray-600 border-gray-200 hover:bg-primary/5 hover:text-primary hover:border-primary/30 transition-all rounded-full px-3.5 py-1.5 h-auto font-medium"
          onClick={() => onSelect(action.text)}
        >
          {action.icon}
          {action.text}
        </Button>
      ))}
    </div>
  );
}
