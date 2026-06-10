import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosError } from 'axios'

/**
 * Trình duyệt: gọi /api/v1 cùng origin → Next.js proxy (next.config rewrites).
 * Tránh lỗi CORS khi dev trên cổng 3001 trong khi Docker chiếm 3000.
 * Server (SSR/API routes): gọi thẳng backend qua API_URL.
 */
function resolveApiBaseUrl(): string {
  if (typeof window !== 'undefined') {
    return '/api/v1'
  }
  const serverUrl =
    process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'
  return `${serverUrl.replace(/\/$/, '')}/api/v1`
}

/**
 * Axios instance chính — dùng cho tất cả API calls.
 * Tự động đính kèm Bearer token và xử lý 401.
 */
const api: AxiosInstance = axios.create({
  baseURL: resolveApiBaseUrl(),
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor: đính kèm JWT token ──────────────
api.interceptors.request.use(
  async (config) => {
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    if (typeof window !== 'undefined') {
      // Try NextAuth session first, fallback to localStorage
      try {
        const { getSession } = await import('next-auth/react')
        const session = await getSession()
        const token = (session as any)?.accessToken
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
      } catch {
        const token = localStorage.getItem('access_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor: xử lý lỗi tập trung ───────────
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
    }

    const data = error.response?.data as {
      message?: string
      errors?: Record<string, string> | string[]
      error?: string
    } | undefined

    const validationFromRecord = data?.errors && !Array.isArray(data.errors) && Object.keys(data.errors).length > 0
      ? Object.values(data.errors)[0]
      : undefined
    const validationFromArray = Array.isArray(data?.errors) && data.errors.length > 0
      ? data.errors.filter(Boolean).join('; ')
      : undefined

    let message =
      validationFromRecord ||
      validationFromArray ||
      data?.message ||
      data?.error ||
      error.message ||
      'Đã xảy ra lỗi, vui lòng thử lại'

    if (error.response?.status === 401) {
      message = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
      if (typeof window !== 'undefined') {
        import('next-auth/react').then(({ signOut }) => {
          signOut({ callbackUrl: '/login' })
        })
      }
    }

    const customError = new Error(message) as Error & { status?: number }
    customError.status = error.response?.status
    return Promise.reject(customError)
  }
)

export default api

export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await api.get<T>(url, config)
  return res.data
}

export async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await api.post<T>(url, data, config)
  return res.data
}

export async function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await api.put<T>(url, data, config)
  return res.data
}

export async function patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await api.patch<T>(url, data, config)
  return res.data
}

export async function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await api.delete<T>(url, config)
  return res.data
}

export type FileUploadResult = {
  url: string
  fileName: string
}

export async function uploadAvatar(file: File): Promise<FileUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await api.post<FileUploadResult>('/files/avatar', formData, {
    timeout: 60_000,
  })
  return res.data
}

export async function uploadStadiumImage(file: File): Promise<FileUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await api.post<FileUploadResult>('/files/stadium', formData, {
    timeout: 60_000,
  })
  return res.data
}
