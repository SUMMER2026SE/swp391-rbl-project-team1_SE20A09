import { post, get, put, del } from '../api';
import { CreateStadiumRequest, StadiumResponse, SportType } from '@/types/stadium';

export const stadiumService = {
  createStadium: (data: CreateStadiumRequest) => {
    return post<StadiumResponse>('/stadiums', data);
  },

  getSportTypes: () => {
    return get<SportType[]>('/sport-types');
  },

  getMyStadiums: () => {
    return get<StadiumResponse[]>('/stadiums/my');
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
