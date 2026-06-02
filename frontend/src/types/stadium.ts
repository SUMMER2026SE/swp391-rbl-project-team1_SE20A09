export interface CreateStadiumRequest {
  stadiumName: string;
  address: string;
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
  sportTypeId: number;
  pricePerHour: number;
  capacity: number;
  description?: string;
  openTime: string; // Format HH:mm:ss
  closeTime: string; // Format HH:mm:ss
}

export interface StadiumResponse {
  stadiumId: number;
  stadiumName: string;
  address: string;
  description: string;
  sportTypeId: number;
  sportName: string;
  pricePerHour: number;
  capacity: number;
  openTime: string;
  closeTime: string;
  stadiumStatus: string;
  averageRating: number;
  imageUrls: string[];
}

export interface SportType {
  sportTypeId: number;
  sportName: string;
}
