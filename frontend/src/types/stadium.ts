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

export interface StadiumResponse {
  stadiumId: number;
  stadiumName: string;
  address: string;
  description: string;
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
