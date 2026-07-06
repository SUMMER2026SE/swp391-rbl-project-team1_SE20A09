import { get, post, del, put } from "@/lib/api";

export interface SportType {
  sportTypeId: number;
  sportName: string;
  nameEn?: string;
  sportCode: string;
  description?: string;
  isActive: boolean;
  isFootballType?: boolean;
  createdAt: string;
}

export interface CreateSportTypeRequest {
  sportName: string;
  nameEn?: string;
  sportCode: string;
  description?: string;
  isActive?: boolean;
  isFootballType?: boolean;
}

export async function fetchSportTypes(): Promise<SportType[]> {
  return get<SportType[]>("/sport-types");
}

export async function createSportType(payload: CreateSportTypeRequest): Promise<SportType> {
  return post<SportType>("/admin/sport-types", payload);
}

export async function updateSportType(id: number, payload: CreateSportTypeRequest): Promise<SportType> {
  return put<SportType>(`/admin/sport-types/${id}`, payload);
}

export async function deleteSportType(id: number): Promise<void> {
  return del<void>(`/admin/sport-types/${id}`);
}
