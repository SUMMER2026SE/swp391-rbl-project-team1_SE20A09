import { Facebook, Instagram, Twitter, Mail, Phone } from "lucide-react";
import { Separator } from "../ui/separator";

export function Footer() {
  return (
    <footer className="bg-card border-t">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <h3 className="mb-4">SportHub</h3>
            <p className="text-sm text-muted-foreground">
              Nền tảng đặt sân thể thao hàng đầu Việt Nam. Tìm kiếm, đặt lịch và kết nối cộng đồng thể thao.
            </p>
          </div>

          <div>
            <h4 className="mb-4">Về chúng tôi</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Giới thiệu
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Liên hệ
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Chính sách bảo mật
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Điều khoản sử dụng
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="mb-4">Dành cho chủ sân</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Đăng ký cho thuê sân
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Hướng dẫn sử dụng
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Chính sách hoa hồng
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="mb-4">Liên hệ</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-center">
                <Phone className="h-4 w-4 mr-2" />
                1900 xxxx
              </li>
              <li className="flex items-center">
                <Mail className="h-4 w-4 mr-2" />
                support@sporthub.vn
              </li>
            </ul>
            <div className="flex space-x-4 mt-4">
              <a href="#" className="text-muted-foreground hover:text-primary transition-colors">
                <Facebook className="h-5 w-5" />
              </a>
              <a href="#" className="text-muted-foreground hover:text-primary transition-colors">
                <Instagram className="h-5 w-5" />
              </a>
              <a href="#" className="text-muted-foreground hover:text-primary transition-colors">
                <Twitter className="h-5 w-5" />
              </a>
            </div>
          </div>
        </div>

        <Separator className="my-8" />

        <div className="text-center text-sm text-muted-foreground">
          © 2026 SportHub. All rights reserved.
        </div>
      </div>
    </footer>
  );
}

