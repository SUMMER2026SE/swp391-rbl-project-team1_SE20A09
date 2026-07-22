"use client";

import Link from "next/link";
import { useState } from "react";
import { Facebook, Instagram, Twitter, Mail, Phone, MapPin, ArrowRight, Loader2 } from "lucide-react";
import { Separator } from "../ui/separator";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { toast } from "sonner";

export function Footer() {
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubscribe = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      toast.error("Vui lòng nhập địa chỉ email của bạn!");
      return;
    }
    
    // Validate email basic
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      toast.error("Vui lòng nhập một địa chỉ email hợp lệ!");
      return;
    }

    setIsSubmitting(true);
    // Giả lập API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    toast.success("Đăng ký nhận tin thành công! Cảm ơn bạn.");
    setEmail("");
    setIsSubmitting(false);
  };

  return (
    <footer className="bg-slate-950 text-slate-200 border-t border-slate-900">
      <div className="container mx-auto px-4 py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 lg:gap-8">
          
          {/* Brand & Newsletter */}
          <div className="space-y-6">
            <div>
              <h3 className="text-2xl font-bold text-white mb-2 tracking-tight">SportsBook<span className="text-primary">.</span></h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Nền tảng đặt sân thể thao hàng đầu Việt Nam. Khám phá, đặt lịch và kết nối đam mê thể thao của bạn một cách dễ dàng nhất.
              </p>
            </div>
            
            <form onSubmit={handleSubscribe} className="space-y-3">
              <h4 className="text-sm font-semibold text-white">Đăng ký nhận tin</h4>
              <div className="flex gap-2">
                <Input 
                  type="email" 
                  placeholder="Email của bạn..." 
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={isSubmitting}
                  className="bg-slate-900 border-slate-800 text-slate-200 focus-visible:ring-primary/50"
                />
                <Button 
                  type="submit" 
                  size="icon" 
                  variant="default" 
                  disabled={isSubmitting}
                  className="shrink-0 hover:scale-105 transition-transform"
                >
                  {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <ArrowRight className="h-4 w-4" />}
                </Button>
              </div>
              <p className="text-xs text-slate-500">Nhận ưu đãi và mã giảm giá mới nhất từ chúng tôi.</p>
            </form>
          </div>

          {/* About Us */}
          <div>
            <h4 className="text-white font-semibold mb-6">Về chúng tôi</h4>
            <ul className="space-y-3 text-sm text-slate-400">
              <li>
                <Link href="/about" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Giới thiệu
                </Link>
              </li>
              <li>
                <Link href="/contact" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Liên hệ
                </Link>
              </li>
              <li>
                <Link href="/privacy" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Chính sách bảo mật
                </Link>
              </li>
              <li>
                <Link href="/terms" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Điều khoản sử dụng
                </Link>
              </li>
            </ul>
          </div>

          {/* For Customers */}
          <div>
            <h4 className="text-white font-semibold mb-6">Dành cho khách hàng</h4>
            <ul className="space-y-3 text-sm text-slate-400">
              <li>
                <Link href="/venues?sport=football" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Tìm sân bóng đá
                </Link>
              </li>
              <li>
                <Link href="/venues?sport=badminton" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Tìm sân cầu lông
                </Link>
              </li>
              <li>
                <Link href="/promotions" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Khuyến mãi & Ưu đãi
                </Link>
              </li>
              <li>
                <Link href="/community" className="hover:text-primary transition-colors flex items-center gap-2 group">
                  <span className="h-1 w-1 rounded-full bg-slate-700 group-hover:bg-primary transition-colors"></span>
                  Cộng đồng thể thao
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact & Owner */}
          <div>
            <h4 className="text-white font-semibold mb-6">Liên hệ & Hỗ trợ</h4>
            <ul className="space-y-4 text-sm text-slate-400">
              <li className="flex items-start gap-3">
                <MapPin className="h-5 w-5 text-primary shrink-0 mt-0.5" />
                <span>Khu Công Nghệ Cao Hòa Lạc, Thạch Thất, Hà Nội</span>
              </li>
              <li className="flex items-center gap-3">
                <Phone className="h-5 w-5 text-primary shrink-0" />
                <a href="tel:19001234" className="hover:text-white transition-colors font-medium">1900 1234</a>
              </li>
              <li className="flex items-center gap-3">
                <Mail className="h-5 w-5 text-primary shrink-0" />
                <a href="mailto:support@sportsbook.vn" className="hover:text-white transition-colors">support@sportsbook.vn</a>
              </li>
            </ul>

            <div className="mt-6 pt-6 border-t border-slate-800">
              <h4 className="text-sm font-semibold text-white mb-3">Hợp tác chủ sân</h4>
              <Link 
                href="/owner/register" 
                className="inline-flex items-center justify-center text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring bg-primary text-primary-foreground shadow hover:bg-primary/90 h-9 px-4 py-2 rounded-md w-full"
              >
                Đăng ký cho thuê sân
              </Link>
            </div>
          </div>
        </div>

        <Separator className="my-8 bg-slate-800" />

        <div className="flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-slate-500">
          <div>
            © {new Date().getFullYear()} SportsBook. All rights reserved.
          </div>
          
          {/* Socials & Payments */}
          <div className="flex items-center gap-8">
            <div className="flex items-center gap-4">
              <a href="#" className="hover:text-primary transition-colors" aria-label="Facebook">
                <Facebook className="h-4 w-4" />
              </a>
              <a href="#" className="hover:text-primary transition-colors" aria-label="Instagram">
                <Instagram className="h-4 w-4" />
              </a>
              <a href="#" className="hover:text-primary transition-colors" aria-label="Twitter">
                <Twitter className="h-4 w-4" />
              </a>
            </div>
            
            <div className="hidden sm:flex items-center gap-2">
              <div className="w-10 h-6 bg-slate-800 rounded flex items-center justify-center text-[10px] font-bold text-slate-400 border border-slate-700">VISA</div>
              <div className="w-10 h-6 bg-slate-800 rounded flex items-center justify-center text-[10px] font-bold text-slate-400 border border-slate-700">ATM</div>
              <div className="w-10 h-6 bg-slate-800 rounded flex items-center justify-center text-[10px] font-bold text-slate-400 border border-slate-700">MOMO</div>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}

