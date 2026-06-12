export interface TimeSlot {
  slotId: number;
  stadiumId: number;
  startTime: string; // HH:mm:ss
  endTime: string; // HH:mm:ss
  pricePerSlot: number;
  slotStatus: "AVAILABLE" | "BOOKED" | "MAINTENANCE";
}

export interface CreateTimeSlotRequest {
  startTime: string;
  endTime: string;
  pricePerSlot: number;
}
