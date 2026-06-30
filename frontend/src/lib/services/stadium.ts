import { post, get, put, del } from '../api';
import {
  CreateStadiumRequest,
  UpdateStadiumRequest,
  StadiumResponse,
  SportType,
  ComplexResponse,
  CreateFacilityRequest,
  CreateCourtRequest,
  BulkTimeSlotRequest,
  CreateTimeSlotRequest,
  CreateComplexRequest
} from '@/types/stadium';
import { TimeSlotDto } from '@/types/complex';

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

  getAllComplexesAdmin: (approvedStatus?: string) => {
    const query = new URLSearchParams();
    if (approvedStatus) query.append('approvedStatus', approvedStatus);
    const queryString = query.toString();
    return get<ComplexResponse[]>(`/complexes${queryString ? `?${queryString}` : ''}`);
  },

  approveComplex: (complexId: number) => {
    return put<ComplexResponse>(`/complexes/${complexId}/approve`, {});
  },

  rejectComplex: (complexId: number, reason: string) => {
    return put<ComplexResponse>(`/complexes/${complexId}/reject`, undefined, {
      params: { reason }
    });
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
  
  getMyComplexes: () => {
    return get<ComplexResponse[]>('/complexes/my');
  },

  createFacility: (data: CreateFacilityRequest) => {
    return post<StadiumResponse>('/stadiums/facilities', data);
  },

  createCourt: (data: CreateCourtRequest) => {
    return post<StadiumResponse>('/stadiums/courts', data);
  },

  bulkCreateSlotsForFacility: (facilityId: number, data: BulkTimeSlotRequest) => {
    return post<TimeSlotDto[]>(`/owner/facilities/${facilityId}/time-slots/bulk`, data);
  },

  bulkCreateSlotsForComplex: (complexId: number, data: BulkTimeSlotRequest) => {
    return post<TimeSlotDto[]>(`/owner/complexes/${complexId}/time-slots/bulk`, data);
  },

  createComplex: (data: CreateComplexRequest) => {
    return post<ComplexResponse>('/complexes', data);
  },

  updateComplex: (complexId: number, data: CreateComplexRequest) => {
    return put<ComplexResponse>(`/complexes/${complexId}`, data);
  },

  updateTimeSlot: (slotId: number, data: CreateTimeSlotRequest) => {
    return put<any>(`/stadiums/time-slots/${slotId}`, data);
  },
};
