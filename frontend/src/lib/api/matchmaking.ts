import api from '../api'

export interface CreateMatchRequestDto {
  stadiumId: number
  sportTypeId: number
  title: string
  description?: string
  playDate: string // YYYY-MM-DD
  startTime: string // HH:MM:SS
  endTime: string // HH:MM:SS
  maxPlayers: number
  skillLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  splitPrice: boolean
  pricePerPlayer?: number
  matchingType?: 'INDIVIDUAL' | 'TEAM_VS_TEAM'
}

export interface MatchResponse {
  matchId: number
  hostName: string
  stadiumName: string
  stadiumAddress: string
  sportName: string
  title: string
  description: string
  playDate: string
  startTime: string
  endTime: string
  maxPlayers: number
  currentPlayers: number
  skillLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  splitPrice: boolean
  pricePerPlayer?: number
  matchStatus: 'OPEN' | 'FULL' | 'CANCELLED' | 'COMPLETED'
  matchingType?: 'INDIVIDUAL' | 'TEAM_VS_TEAM'
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  pageNo: number
  pageSize: number
  totalElements: number
  totalPages: number
  last: boolean
}

export async function createMatchRequest(data: CreateMatchRequestDto): Promise<MatchResponse> {
  const res = await api.post<MatchResponse>('/matchmaking', data)
  return res.data
}

export async function getActiveMatches(page = 0, size = 10): Promise<PageResponse<MatchResponse>> {
  const res = await api.get<PageResponse<MatchResponse>>('/matchmaking', {
    params: { page, size }
  })
  return res.data
}

export async function joinMatchRequest(matchId: number, message = ''): Promise<{ message: string }> {
  const res = await api.post<{ message: string }>(`/matchmaking/${matchId}/join`, null, {
    params: { message }
  })
  return res.data
}
