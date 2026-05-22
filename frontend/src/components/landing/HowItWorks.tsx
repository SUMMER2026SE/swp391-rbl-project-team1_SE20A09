import { Search, Calendar, CheckCircle } from "lucide-react";

interface StepProps {
  icon: React.ReactNode;
  number: string;
  title: string;
  description: string;
}

function Step({ icon, number, title, description }: StepProps) {
  return (
    <div className="flex flex-col items-center text-center">
      <div className="relative mb-4">
        <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center">
          <div className="text-primary">{icon}</div>
        </div>
        <div className="absolute -top-2 -right-2 w-8 h-8 bg-primary text-primary-foreground rounded-full flex items-center justify-center">
          {number}
        </div>
      </div>
      <h3 className="mb-2">{title}</h3>
      <p className="text-muted-foreground">{description}</p>
    </div>
  );
}

export function HowItWorks() {
  return (
    <section className="py-16 bg-muted/30">
      <div className="container mx-auto px-4">
        <h2 className="text-center mb-12">Cách thức hoạt động</h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
          <Step
            icon={<Search className="h-10 w-10" />}
            number="1"
            title="Tìm kiếm sân"
            description="Tìm sân phù hợp với vị trí, thời gian và môn thể thao yêu thích"
          />

          <Step
            icon={<Calendar className="h-10 w-10" />}
            number="2"
            title="Đặt lịch"
            description="Chọn khung giờ phù hợp và thanh toán trực tuyến an toàn"
          />

          <Step
            icon={<CheckCircle className="h-10 w-10" />}
            number="3"
            title="Chơi thể thao"
            description="Nhận xác nhận và tận hưởng trận đấu của bạn"
          />
        </div>
      </div>
    </section>
  );
}

