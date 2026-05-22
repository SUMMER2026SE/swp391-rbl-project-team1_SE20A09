'use client'

import { Search, MapPin, Calendar, Clock } from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";

export function SearchBar() {
  return (
    <div className="w-full max-w-5xl mx-auto bg-white rounded-lg shadow-lg p-6">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="relative">
          <MapPin className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
          <Input
            placeholder="Địa điểm"
            className="pl-10"
          />
        </div>

        <Select>
          <SelectTrigger>
            <SelectValue placeholder="Loại sân" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="football">Bóng đá</SelectItem>
            <SelectItem value="badminton">Cầu lông</SelectItem>
            <SelectItem value="tennis">Quần vợt</SelectItem>
            <SelectItem value="basketball">Bóng rổ</SelectItem>
          </SelectContent>
        </Select>

        <div className="relative">
          <Calendar className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
          <Input
            type="date"
            placeholder="Ngày"
            className="pl-10"
          />
        </div>

        <div className="relative">
          <Clock className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
          <Select>
            <SelectTrigger>
              <SelectValue placeholder="Giờ" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="06-08">06:00 - 08:00</SelectItem>
              <SelectItem value="08-10">08:00 - 10:00</SelectItem>
              <SelectItem value="10-12">10:00 - 12:00</SelectItem>
              <SelectItem value="14-16">14:00 - 16:00</SelectItem>
              <SelectItem value="16-18">16:00 - 18:00</SelectItem>
              <SelectItem value="18-20">18:00 - 20:00</SelectItem>
              <SelectItem value="20-22">20:00 - 22:00</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <Button className="w-full md:w-auto mt-4 md:mt-0 md:absolute md:right-6 md:bottom-6">
        <Search className="mr-2 h-5 w-5" />
        Tìm kiếm
      </Button>
    </div>
  );
}

