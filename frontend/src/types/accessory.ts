export interface Accessory {
  accessoryId: number;
  stadiumId: number;
  stadiumName: string;
  name: string;
  pricePerUnit: number;
  quantity: number;
  isAvailable: boolean;
}

export interface CreateAccessoryRequest {
  name: string;
  pricePerUnit: number;
  quantity: number;
  isAvailable: boolean;
}
