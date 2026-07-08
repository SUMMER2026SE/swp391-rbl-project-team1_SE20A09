export enum FootballFieldType {
  FIVE_A_SIDE = 'FIVE_A_SIDE',
  SEVEN_A_SIDE = 'SEVEN_A_SIDE',
  ELEVEN_A_SIDE = 'ELEVEN_A_SIDE',
  FUTSAL = 'FUTSAL'
}

export interface CreateStadiumRequest {
  stadiumName: string;
  address: string;
  latitude: number;
  longitude: number;
  sportTypeId: number;
  pricePerHour: number;
  description?: string;
  openTime?: string; // Format HH:mm:ss
  closeTime?: string; // Format HH:mm:ss
  imageUrls?: string[];
}

export interface UpdateStadiumRequest {
  stadiumName: string;
  address: string;
  latitude: number;
  longitude: number;
  sportTypeId: number;
  pricePerHour: number;
  description?: string;
  openTime: string; // Format HH:mm:ss
  closeTime: string; // Format HH:mm:ss
  imageUrls?: string[];
  footballFieldType?: FootballFieldType | null;
}

export interface StadiumResponse {
  stadiumId: number;
  stadiumName: string;
  address: string;
  latitude: number;
  longitude: number;
  description: string;
  sportTypeId: number;
  sportName: string;
  isFootballType?: boolean;
  pricePerHour: number;
  openTime: string;
  closeTime: string;
  stadiumStatus: string;
  averageRating: number;
  totalReviews?: number;
  imageUrls: string[];
  firstImageUrl?: string | null;
  approvedStatus: string;
  nodeType?: 'FACILITY' | 'COURT' | null;
  complexId?: number | null;
  parentStadiumId?: number | null;
  /** True nếu sân đang bị chặn đặt HÔM NAY do bất kỳ cơ chế bảo trì nào (kể cả khung ngày, dù stadiumStatus vẫn AVAILABLE). Chỉ có ở API dành cho Owner. */
  underMaintenanceToday?: boolean | null;
  footballFieldType?: FootballFieldType | null;
}

export interface ComplexResponse {
  complexId: number;
  ownerId: number;
  name: string;
  description?: string | null;
  address: string;
  phone?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  coverImageUrl?: string | null;
  complexStatus: string;
  approvedStatus: string;
  rejectionReason?: string | null;
  averageRating?: number | null;
  reviewCount?: number;
  sportTypeIds?: number[];
  sportNames?: string[];
  amenityIds?: number[];
  imageUrls?: string[];
  createdAt?: string;
}

export interface CreateFacilityRequest {
  complexId: number;
  stadiumName: string;
  description?: string;
  sportTypeId: number;
  openTime: string;  // Format "HH:mm:ss"
  closeTime: string; // Format "HH:mm:ss"
  imageUrls?: string[];
}

export interface CreateCourtRequest {
  parentStadiumId: number;
  stadiumName: string;
  description?: string;
  pricePerHour: number;
  imageUrls?: string[];
  footballFieldType?: FootballFieldType | null;
}

export interface CreateTimeSlotRequest {
  startTime: string;  // Format "HH:mm:ss"
  endTime: string;    // Format "HH:mm:ss"
  pricePerSlot: number;
}

export interface BulkTimeSlotRequest {
  courtIds?: number[];
  facilityIds?: number[];
  applyToAllCourts?: boolean;
  slots: CreateTimeSlotRequest[];
}

export interface SportType {
  sportTypeId: number;
  sportName: string;
  isFootballType?: boolean;
}

export interface CreateComplexRequest {
  name: string;
  description?: string;
  address: string;
  phone?: string;
  latitude: number;
  longitude: number;
  coverImageUrl?: string;
  sportTypeIds?: number[];
  amenityIds?: number[];
  imageUrls?: string[];
}

export interface CreateMaintenanceScheduleRequest {
  startDate: string; // Format "yyyy-MM-dd"
  endDate?: string;  // Format "yyyy-MM-dd" — bỏ trống = vô thời hạn
  startTime?: string; // Format "HH:mm" — bỏ trống = tính từ đầu ngày startDate
  endTime?: string;   // Format "HH:mm" — bỏ trống = tính đến hết ngày endDate. Chỉ hợp lệ khi có endDate
  reason?: string;
}

export interface MaintenanceScheduleResponse {
  maintenanceId: number;
  stadiumId: number | null;
  complexId: number | null;
  sportName: string;
  firstImageUrl: string | null;
  startDate: string;
  endDate: string | null;
  startTime: string | null;
  endTime: string | null;
  reason: string | null;
  indefinite: boolean;
  active: boolean;
  createdAt: string;
}

export interface MaintenanceSchedulePage {
  content: MaintenanceScheduleResponse[];
  totalElements: number;
  totalPages: number;
}
