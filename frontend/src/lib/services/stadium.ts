import { post, get, put, del } from '../api';
import { CreateStadiumRequest, UpdateStadiumRequest, StadiumResponse, SportType } from '@/types/stadium';

export const stadiumService = {
  createStadium: (data: CreateStadiumRequest) => {
    return post<StadiumResponse>('/stadiums', data);
  },

  getSportTypes: () => {
    return get<SportType[]>('/sport-types');
  },

  getMyStadiums: (params?: { search?: string; sportTypeId?: number; status?: string }) => {
    const query = new URLSearchParams();
    if (params?.search) query.append('search', params.search);
    if (params?.sportTypeId) query.append('sportTypeId', params.sportTypeId.toString());
    if (params?.status) query.append('status', params.status);
    const queryString = query.toString();
    return get<StadiumResponse[]>(`/stadiums/my${queryString ? `?${queryString}` : ''}`);
  },

  getStadiumById: (stadiumId: number) => {
    return get<StadiumResponse>(`/stadiums/${stadiumId}`);
  },

  updateStadium: (stadiumId: number, data: UpdateStadiumRequest) => {
    return put<StadiumResponse>(`/stadiums/${stadiumId}`, data);
  },

  getAllStadiums: (approvedStatus?: string) => {
    const query = new URLSearchParams();
    if (approvedStatus) query.append('approvedStatus', approvedStatus);
    const queryString = query.toString();
    return get<StadiumResponse[]>(`/stadiums${queryString ? `?${queryString}` : ''}`);
  },

  approveStadium: (stadiumId: number) => {
    return put<StadiumResponse>(`/stadiums/${stadiumId}/approve`, {});
  },

  rejectStadium: (stadiumId: number) => {
    return put<StadiumResponse>(`/stadiums/${stadiumId}/reject`, {});
  },

  suspendStadium: (stadiumId: number) => {
    return put<void>(`/stadiums/${stadiumId}/suspend`);
  },

  activateStadium: (stadiumId: number) => {
    return put<void>(`/stadiums/${stadiumId}/activate`);
  },

  deleteStadium: (stadiumId: number) => {
    return del<void>(`/stadiums/${stadiumId}`);
  },
};
