import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";

export default function AboutPage() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Header />
      <main className="flex-grow container mx-auto px-4 py-12">
        <div className="bg-white rounded-xl shadow-sm border p-8 md:p-12 max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold mb-6 text-slate-900">Giới thiệu về SportsBook</h1>
          <div className="prose prose-slate max-w-none space-y-4">
            <p className="text-slate-600 leading-relaxed">
              Chào mừng bạn đến với <strong>SportsBook</strong> - nền tảng đặt sân thể thao trực tuyến hàng đầu Việt Nam.
              Chúng tôi ra đời với sứ mệnh kết nối những người đam mê thể thao với các cơ sở vật chất chất lượng, giúp việc tìm kiếm và đặt sân trở nên dễ dàng, nhanh chóng và minh bạch hơn bao giờ hết.
            </p>
            <h2 className="text-xl font-semibold text-slate-800 mt-6">Tầm nhìn & Sứ mệnh</h2>
            <p className="text-slate-600 leading-relaxed">
              Tầm nhìn của chúng tôi là xây dựng một cộng đồng thể thao phát triển mạnh mẽ, nơi mọi người có thể dễ dàng tiếp cận với thể thao mỗi ngày.
              Sứ mệnh của SportsBook là cung cấp giải pháp công nghệ toàn diện cho cả người chơi (khách hàng) và người cho thuê (chủ sân), tối ưu hóa thời gian và trải nghiệm.
            </p>
            <h2 className="text-xl font-semibold text-slate-800 mt-6">Dịch vụ của chúng tôi</h2>
            <ul className="list-disc pl-5 text-slate-600 space-y-2">
              <li>Hệ thống tìm kiếm và lọc sân thể thao thông minh theo vị trí, loại hình (Bóng đá, cầu lông, tennis, v.v.).</li>
              <li>Theo dõi tình trạng trống của sân theo thời gian thực.</li>
              <li>Thanh toán trực tuyến nhanh chóng, bảo mật.</li>
              <li>Cung cấp công cụ quản lý sân toàn diện dành riêng cho chủ sân.</li>
            </ul>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
