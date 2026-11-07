// Mirrors the response records of the services. Only the fields the client actually
// uses are declared - an extra field in a response is simply ignored.

export type Role = "CLIENT" | "PROVIDER" | "ADMIN";

export interface User {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  activated: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface ServiceOffering {
  id: string;
  providerId: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
}

export type SlotStatus = "AVAILABLE" | "BOOKED" | "BLOCKED";

export interface Slot {
  id: string;
  serviceId: string;
  providerId: string;
  startTime: string;
  endTime: string;
  status: SlotStatus;
}

export type ReservationStatus = "CONFIRMED" | "CANCELLED" | "COMPLETED";

export interface Reservation {
  id: string;
  clientId: string;
  providerId: string;
  serviceId: string;
  slotId: string;
  serviceName: string | null;
  startTime: string;
  endTime: string;
  status: ReservationStatus;
  note: string | null;
  rewardId: string | null;
  createdAt: string;
  cancelledAt: string | null;
}

export interface LoyaltyRule {
  id: string;
  providerId: string;
  requiredAppointments: number;
  rewardDescription: string;
  rewardValidityDays: number;
  active: boolean;
}

export interface LoyaltyAccount {
  clientId: string;
  providerId: string;
  appointmentsTowardsReward: number;
  totalAppointments: number;
  requiredAppointments: number | null;
  appointmentsUntilReward: number | null;
}

export type RewardStatus = "AVAILABLE" | "REDEEMED" | "EXPIRED";

export interface LoyaltyReward {
  id: string;
  clientId: string;
  providerId: string;
  description: string;
  status: RewardStatus;
  usable: boolean;
  issuedAt: string;
  expiresAt: string;
  redeemedAt: string | null;
  reservationId: string | null;
}

export interface Notification {
  id: string;
  templateCode: string;
  recipientEmail: string;
  subject: string;
  body: string;
  status: "PENDING" | "SENT" | "FAILED";
  attempts: number;
  createdAt: string;
  sentAt: string | null;
}

export interface NotificationTemplate {
  id: string;
  code: string;
  description: string | null;
  subject: string;
  body: string;
  parameters: string[];
  active: boolean;
}

/** Spring serialises Page as a PagedModel: the rows plus a page descriptor. */
export interface PagedResponse<T> {
  content: T[];
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
