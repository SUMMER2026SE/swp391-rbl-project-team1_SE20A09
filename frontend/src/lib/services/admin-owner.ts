import { get, post } from '../api';
import { ApiResponse, PageResponse } from '@/types/common';
import { AxiosRequestConfig } from 'axios';

export interface OwnerDetail {
  ownerId: number;
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  businessName: string;
  taxCode: string;
  businessAddress: string;
  approvedStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
  businessLicenseUrl?: string;
  identityCardUrl?: string;
  createdAt: string;
}

export const adminOwnerService = {
  getRegistrations: (status: 'PENDING' | 'APPROVED' | 'REJECTED', page = 0, pageSize = 10, config?: AxiosRequestConfig) => {
    return get<ApiResponse<PageResponse<OwnerDetail>>>(`/admin/owners/registrations?status=${status}&page=${page}&pageSize=${pageSize}`, config);
  },

  approveOrReject: (ownerId: number, data: { approvedStatus: 'APPROVED' | 'REJECTED'; rejectionReason?: string }) => {
    return post<ApiResponse<OwnerDetail>>(`/admin/owners/${ownerId}/approve`, data);
  }
};
