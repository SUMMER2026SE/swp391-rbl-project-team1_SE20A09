import { get, post } from "@/lib/api";

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
