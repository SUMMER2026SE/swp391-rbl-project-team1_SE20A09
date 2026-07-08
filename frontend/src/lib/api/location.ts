import api from '../api'

export interface SupportedLocationDto {
  province: string
  districts: string[]
}

export async function getLocations(): Promise<SupportedLocationDto[]> {
  const res = await api.get<SupportedLocationDto[]>('/public/locations')
  return res.data
}
