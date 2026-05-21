export default function HomePage() {
  return (
    <main className="min-h-screen flex flex-col items-center justify-center bg-slate-900 relative overflow-hidden">
      {/* Background glow effects */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-green-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-[400px] h-[400px] bg-emerald-500/5 rounded-full blur-3xl" />
      </div>

      {/* Content */}
      <div className="relative z-10 text-center px-6 max-w-2xl mx-auto">
        {/* Logo / Icon */}
        <div className="text-7xl mb-6 animate-bounce">🏟️</div>

        {/* Heading */}
        <h1 className="text-4xl md:text-5xl font-bold mb-4 leading-tight">
          <span className="text-gradient">Sport Venue</span>
          <br />
          <span className="text-slate-200">Management System</span>
        </h1>

        <p className="text-slate-400 text-lg mb-8 leading-relaxed">
          Nền tảng đặt sân thể thao trực tuyến — tìm kiếm, đặt lịch, thanh toán và kết nối cộng đồng thể thao trong một ứng dụng duy nhất.
        </p>

        {/* Status badges */}
        <div className="flex flex-wrap gap-3 justify-center mb-10">
          <span className="badge-green">✅ Frontend Running</span>
          <span className="badge badge-yellow">🔧 In Development</span>
          <span className="badge badge-green">Next.js 14</span>
        </div>

        {/* Quick links */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <a
            href="http://localhost:8080/swagger-ui.html"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-primary inline-flex items-center gap-2"
          >
            📄 API Docs (Swagger)
          </a>
          <a
            href="http://localhost:8080/api/v1/hello"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-outline inline-flex items-center gap-2"
          >
            🔍 Test Backend API
          </a>
        </div>

        {/* Tech stack */}
        <div className="mt-12 card p-6">
          <p className="text-sm text-slate-500 mb-4 font-medium uppercase tracking-wider">Tech Stack</p>
          <div className="grid grid-cols-3 gap-3 text-sm">
            {[
              { icon: '⚡', label: 'Next.js 14' },
              { icon: '☕', label: 'Spring Boot 3.3' },
              { icon: '🐘', label: 'PostgreSQL 16' },
              { icon: '⚡', label: 'Redis 7' },
              { icon: '🔐', label: 'Spring Security' },
              { icon: '📦', label: 'Docker' },
            ].map((item) => (
              <div
                key={item.label}
                className="glass rounded-lg p-2.5 flex flex-col items-center gap-1 hover:border-green-500/50 transition-colors"
              >
                <span className="text-xl">{item.icon}</span>
                <span className="text-slate-400 text-xs">{item.label}</span>
              </div>
            ))}
          </div>
        </div>

        <p className="mt-8 text-slate-600 text-sm">
          Team 1 — SE20A09 · SWP391 · FPT University
        </p>
      </div>
    </main>
  )
}
