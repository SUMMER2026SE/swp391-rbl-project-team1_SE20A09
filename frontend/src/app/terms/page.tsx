import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";

export default function TermsPage() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Header />
      <main className="flex-grow container mx-auto px-4 py-12">
        <div className="bg-white rounded-xl shadow-sm border p-8 md:p-12 max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold mb-6 text-slate-900">Điều khoản sử dụng</h1>
          <div className="prose prose-slate max-w-none space-y-4 text-slate-600">
            <p className="text-sm text-slate-500 mb-6">Cập nhật lần cuối: 22/07/2026</p>
            
            <h2 className="text-xl font-semibold text-slate-800 mt-6">1. Chấp nhận điều khoản</h2>
            <p>
              Bằng việc đăng ký, truy cập và sử dụng dịch vụ của SportsBook, bạn đồng ý tuân thủ toàn bộ các điều khoản và điều kiện được nêu tại đây. 
              Nếu bạn không đồng ý với bất kỳ điều khoản nào, vui lòng không sử dụng nền tảng của chúng tôi.
            </p>

            <h2 className="text-xl font-semibold text-slate-800 mt-6">2. Quy định đối với người dùng (Khách hàng)</h2>
            <ul className="list-disc pl-5 space-y-1">
              <li>Cung cấp thông tin cá nhân chính xác khi đăng ký và đặt sân.</li>
              <li>Thực hiện thanh toán đầy đủ theo quy định của sân đối với các đơn đặt có yêu cầu cọc hoặc thanh toán trước.</li>
              <li>Tuân thủ các nội quy và quy định cụ thể của từng sân thể thao khi đến sử dụng dịch vụ.</li>
              <li>SportsBook có quyền hủy đơn đặt sân nếu phát hiện dấu hiệu gian lận hoặc vi phạm.</li>
            </ul>

            <h2 className="text-xl font-semibold text-slate-800 mt-6">3. Chính sách hủy và hoàn tiền</h2>
            <p>
              Việc hủy lịch và hoàn tiền tuân theo chính sách riêng của hệ thống và của từng cơ sở thể thao.
              Thông thường, bạn có thể hủy miễn phí nếu thực hiện trước giờ đá một khoảng thời gian nhất định (ví dụ: 12 tiếng).
              Sau thời gian này, phí hủy có thể được áp dụng.
            </p>

            <h2 className="text-xl font-semibold text-slate-800 mt-6">4. Giới hạn trách nhiệm</h2>
            <p>
              SportsBook đóng vai trò là nền tảng trung gian kết nối. Chúng tôi không chịu trách nhiệm pháp lý đối với các tranh chấp phát sinh giữa chủ sân và người chơi tại địa điểm thi đấu, 
              nhưng sẽ hỗ trợ cung cấp thông tin giao dịch để giải quyết vấn đề một cách minh bạch.
            </p>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
