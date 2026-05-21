---
trigger: always_on
---

# GEMINI.md — Cấu hình Agent SportVenue

Tệp này là điểm khởi đầu cấu hình cho AI Agent hỗ trợ nhóm phát triển hệ thống **Sport Venue Management**.
Các quy tắc, sub-agents, và skills đã được tổ chức trong `.gemini/`.

## Cấu trúc `.gemini/`

```
.gemini/
├── rules/
│   ├── agent-identity.md       ← danh tính agent, hành vi SME
│   ├── language-protocol.md    ← giao thức ngôn ngữ (Việt/Anh)
│   ├── project-context.md      ← tổng quan dự án SportVenue
│   ├── coding-guidelines.md    ← quy tắc code Java + TypeScript
│   ├── skills-system.md        ← danh sách skills khả dụng
│   ├── workflow.md             ← Git flow, commit convention
│   └── design.md              ← nguyên tắc UI/UX
│
├── agents/
│   ├── researcher.md           ← nghiên cứu công nghệ, đánh giá giải pháp
│   ├── reviewer.md             ← review code, security, performance
│   ├── frontend-architect.md   ← thiết kế & xây dựng UI premium
│   └── backend-guardian.md     ← bảo vệ backend Java: security + performance
│
└── skills/
    ├── spring-boot/            ← Spring Boot 3.3, JPA, Security, Flyway
    ├── java-patterns/          ← Java 17 modern patterns
    ├── jwt-auth/               ← JWT + OAuth2 implementation
    ├── nextjs-best-practices/  ← Next.js 14 App Router
    ├── react-patterns/         ← React hooks, composition patterns
    ├── typescript-pro/         ← TypeScript strict patterns
    ├── tailwind-patterns/      ← Tailwind CSS v3
    ├── zustand-store-ts/       ← Zustand state management
    ├── shadcn/                 ← Shadcn/ui components
    ├── frontend-design/        ← Design thinking, aesthetics
    ├── postgresql/             ← PostgreSQL + Flyway migrations
    ├── docker-expert/          ← Docker + Docker Compose
    ├── github-actions-templates/ ← CI/CD workflows
    ├── security-auditor/       ← Security audit & hardening
    ├── systematic-debugging/   ← Root cause analysis
    └── powershell-windows/     ← Windows PowerShell scripting
```

## Nguyên tắc cốt lõi

- Luôn ưu tiên **tính nhất quán** với code hiện có trong codebase
- **Không tự ý thay đổi** kiến trúc mà không có sự đồng ý
- Mọi đề xuất phải kèm theo **giải thích ngắn gọn** bằng tiếng Việt
- Khi không chắc chắn → **hỏi trước, code sau**
