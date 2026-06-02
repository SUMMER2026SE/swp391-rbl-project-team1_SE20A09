import { post, get, put } from '../api';
import { CreateStadiumRequest, UpdateStadiumRequest, StadiumResponse, SportType } from '@/types/stadium';

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

  getStadiumById: (stadiumId: number) => {
    return get<StadiumResponse[]>(`/stadiums/my`).then(
      (stadiums) => {
        const stadium = stadiums.find((s) => s.stadiumId === stadiumId);
        if (!stadium) throw new Error('Stadium not found');
        return stadium;
      }
    );
  },

  updateStadium: (stadiumId: number, data: UpdateStadiumRequest) => {
    return put<StadiumResponse>(`/stadiums/${stadiumId}`, data);
  },
};
