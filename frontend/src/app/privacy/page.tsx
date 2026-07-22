import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";

export default function PrivacyPage() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Header />
      <main className="flex-grow container mx-auto px-4 py-12">
        <div className="bg-white rounded-xl shadow-sm border p-8 md:p-12 max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold mb-6 text-slate-900">Chính sách bảo mật</h1>
          <div className="prose prose-slate max-w-none space-y-4 text-slate-600">
            <p className="text-sm text-slate-500 mb-6">Cập nhật lần cuối: 22/07/2026</p>
            
            <h2 className="text-xl font-semibold text-slate-800 mt-6">1. Thu thập thông tin</h2>
            <p>Chúng tôi thu thập các thông tin cá nhân cơ bản khi bạn đăng ký tài khoản hoặc sử dụng dịch vụ trên SportsBook, bao gồm:</p>
            <ul className="list-disc pl-5 space-y-1">
              <li>Họ và tên, địa chỉ email, số điện thoại.</li>
              <li>Lịch sử đặt sân và giao dịch.</li>
              <li>Thông tin thiết bị và địa chỉ IP nhằm mục đích bảo mật.</li>
            </ul>

            <h2 className="text-xl font-semibold text-slate-800 mt-6">2. Sử dụng thông tin</h2>
            <p>Thông tin của bạn được sử dụng để:</p>
            <ul className="list-disc pl-5 space-y-1">
              <li>Xác nhận đặt lịch và thông báo tới chủ sân.</li>
              <li>Hỗ trợ khách hàng và giải quyết khiếu nại (nếu có).</li>
              <li>Gửi email thông báo về lịch đặt sân, khuyến mãi hoặc thay đổi về dịch vụ.</li>
            </ul>

            <h2 className="text-xl font-semibold text-slate-800 mt-6">3. Bảo vệ và chia sẻ thông tin</h2>
            <p>
              SportsBook cam kết không bán, trao đổi hoặc chia sẻ thông tin cá nhân của bạn cho bên thứ ba vì mục đích thương mại.
              Thông tin chỉ được chia sẻ cho chủ sân (Họ tên, Số điện thoại) để xác nhận khi bạn đặt lịch tại sân của họ.
            </p>

            <h2 className="text-xl font-semibold text-slate-800 mt-6">4. Quyền lợi của bạn</h2>
            <p>
              Bạn có quyền truy cập, chỉnh sửa hoặc yêu cầu xóa bỏ thông tin cá nhân của mình bất kỳ lúc nào thông qua phần "Hồ sơ" hoặc bằng cách liên hệ trực tiếp với chúng tôi qua email hỗ trợ.
            </p>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
