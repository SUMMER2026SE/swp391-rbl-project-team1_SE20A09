import { NextRequest, NextResponse } from 'next/server';
import { getToken } from 'next-auth/jwt';

export const dynamic = 'force-dynamic';

export async function POST(req: NextRequest) {
  try {
    const backendUrl = `${process.env.API_URL || 'http://backend:8080'}/api/v1/ai/chat`;

    // Dùng getToken() — đọc JWT token trực tiếp từ cookie session,
    // hoạt động đáng tin cậy hơn getServerSession() trong Docker production build
    const token = await getToken({
      req,
      secret: process.env.NEXTAUTH_SECRET,
    });

    const serverToken = (token as any)?.accessToken;

    // Fallback: dùng Authorization header từ client nếu cookie session không có
    const clientAuthHeader = req.headers.get('authorization');
    const authHeader = serverToken
      ? `Bearer ${serverToken}`
      : clientAuthHeader;

    if (!authHeader) {
      console.error('[AI Chat Proxy] No auth token found — session:', !!token, 'clientHeader:', !!clientAuthHeader);
      return new NextResponse('Unauthorized', { status: 401 });
    }

    const body = await req.json();

    const response = await fetch(backendUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('[AI Chat Proxy] Backend error:', response.status, errorText);
      return new NextResponse(errorText, { status: response.status });
    }

    const stream = response.body || new ReadableStream();

    return new NextResponse(stream, {
      headers: {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'X-Vercel-AI-Data-Stream': 'v1',
        'Cache-Control': 'no-cache, no-transform',
        'Connection': 'keep-alive',
      },
    });
  } catch (error: any) {
    console.error('[AI Chat Proxy] Unexpected error:', error.message);
    return new NextResponse(error.message || 'Internal Server Error', { status: 500 });
  }
}
