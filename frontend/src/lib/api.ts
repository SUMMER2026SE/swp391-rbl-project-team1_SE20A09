import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosError } from 'axios'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

/**
 * Axios instance chính — dùng cho tất cả API calls.
 * Tự động đính kèm Bearer token và xử lý 401.
 */
const api: AxiosInstance = axios.create({
  baseURL: `${BASE_URL}/api/v1`,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor: đính kèm JWT token ──────────────
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('access_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
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

    // 401: thử refresh token một lần
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      // TODO: implement token refresh logic
      // const newToken = await refreshToken()
      // if (newToken) { ... retry ... }
    }

    // Chuẩn hóa error message (ưu tiên chi tiết validation từ BE)
    const data = error.response?.data as {
      message?: string
      errors?: string[]
      error?: string
    } | undefined
    const validationDetail = data?.errors?.filter(Boolean).join('; ')
    const message =
      validationDetail ||
      data?.message ||
      data?.error ||
      error.message ||
      'Đã xảy ra lỗi, vui lòng thử lại'

    const customError = new Error(message) as any;
    customError.status = error.response?.status;
    return Promise.reject(customError)
  }
)

export default api

// ── Typed helpers ─────────────────────────────────────────

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
