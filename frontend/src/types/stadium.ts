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
  pricePerHour: number;
  openTime: string;
  closeTime: string;
  stadiumStatus: string;
  averageRating: number;
  imageUrls: string[];
  approvedStatus: string;
  capacity?: number;
}

export interface SportType {
  sportTypeId: number;
  sportName: string;
}
