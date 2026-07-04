// Deprecated. AI Chat logic has been migrated to Spring Boot backend (/api/v1/ai/chat).
export async function POST(req: Request) {
  return new Response("Migrated to Spring Boot", { status: 410 });
}
