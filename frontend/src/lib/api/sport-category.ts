import { get, post } from "@/lib/api";

export interface SportType {
  sportTypeId: number;
  sportName: string;
  nameEn?: string;
  icon: string;
  sportCode: string;
  description?: string;
  isActive: boolean;
  isFootballType?: boolean;
  createdAt: string;
  fieldTypes?: string[];
  internalNote?: string;
  priority?: number;
}

export interface CreateSportTypeRequest {
  sportName: string;
  nameEn?: string;
  icon?: string;
  sportCode: string;
  description?: string;
  isActive?: boolean;
  isFootballType?: boolean;
  fieldTypes?: string[];
  internalNote?: string;
  priority?: number;
}

export async function fetchSportTypes(): Promise<SportType[]> {
  return get<SportType[]>("/sport-types");
}

export async function createSportType(payload: CreateSportTypeRequest): Promise<SportType> {
  return post<SportType>("/admin/sport-types", payload);
}
