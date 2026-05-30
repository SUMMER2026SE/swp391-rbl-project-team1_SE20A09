import { post, get } from '../api';
import { CreateStadiumRequest, StadiumResponse, SportType } from '@/types/stadium';

export const stadiumService = {
  createStadium: (data: CreateStadiumRequest) => {
    return post<StadiumResponse>('/stadiums', data);
  },
  
  getSportTypes: () => {
    return get<SportType[]>('/sport-types');
  }
};
