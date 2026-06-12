import api from '../api'
import { TimeSlot, CreateTimeSlotRequest } from '../../types/timeSlot'

export async function getStadiumTimeSlots(stadiumId: number): Promise<TimeSlot[]> {
  const res = await api.get<TimeSlot[]>(`/stadiums/${stadiumId}/time-slots`)
  return res.data
}

export async function createTimeSlot(stadiumId: number, data: CreateTimeSlotRequest): Promise<TimeSlot> {
  const res = await api.post<TimeSlot>(`/stadiums/${stadiumId}/time-slots`, data)
  return res.data
}

export async function bulkCreateTimeSlots(stadiumId: number, data: CreateTimeSlotRequest[]): Promise<TimeSlot[]> {
  const res = await api.post<TimeSlot[]>(`/stadiums/${stadiumId}/time-slots/bulk`, data)
  return res.data
}

export async function deleteTimeSlot(slotId: number): Promise<void> {
  await api.delete(`/stadiums/time-slots/${slotId}`)
}

export async function toggleTimeSlot(slotId: number): Promise<TimeSlot> {
  const res = await api.patch<TimeSlot>(`/stadiums/time-slots/${slotId}/toggle`)
  return res.data
}
