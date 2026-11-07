import type {
  AuthResponse,
  LoyaltyAccount,
  LoyaltyReward,
  LoyaltyRule,
  Notification,
  NotificationTemplate,
  PagedResponse,
  Reservation,
  Role,
  ServiceOffering,
  Slot,
  SlotStatus,
  User,
} from "./types";

const TOKEN_KEY = "booking.accessToken";

export function storedToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function storeToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;

  anonymous?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  const token = storedToken();

  if (token && !options.anonymous) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(`/api${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const payload = text ? safeParse(text) : undefined;

  if (!response.ok) {
    const message =
      (payload as { message?: string } | undefined)?.message ??
      `${response.status} ${response.statusText}`;
    const fieldErrors = (payload as { fieldErrors?: Record<string, string> } | undefined)?.fieldErrors;

    if (response.status === 401) {
      storeToken(null);
    }
    throw new ApiError(response.status, message, fieldErrors ?? undefined);
  }

  return payload as T;
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

export interface RegisterInput {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  role: Role;
}

export const api = {
  register: (input: RegisterInput) =>
    request<User>("/auth/register", { method: "POST", body: input, anonymous: true }),

  activate: (token: string) =>
    request<AuthResponse>(`/auth/activate?token=${encodeURIComponent(token)}`, {
      method: "POST",
      anonymous: true,
    }),

  login: (email: string, password: string) =>
    request<AuthResponse>("/auth/login", {
      method: "POST",
      body: { email, password },
      anonymous: true,
    }),

  requestPasswordReset: (email: string) =>
    request<void>("/auth/password-reset-request", {
      method: "POST",
      body: { email },
      anonymous: true,
    }),

  resetPassword: (token: string, newPassword: string) =>
    request<void>("/auth/password-reset", {
      method: "POST",
      body: { token, newPassword },
      anonymous: true,
    }),

  me: () => request<User>("/users/me"),

  users: () => request<User[]>("/users"),

  setUserEnabled: (id: string, enabled: boolean) =>
    request<User>(`/users/${id}/enabled`, { method: "PUT", body: { enabled } }),

  setUserRole: (id: string, role: Role) =>
    request<User>(`/users/${id}/role`, { method: "PUT", body: { role } }),

  services: (providerId?: string) =>
    request<ServiceOffering[]>(
      providerId ? `/services?providerId=${providerId}` : "/services",
      { anonymous: true },
    ),

  createService: (input: {
    name: string;
    description?: string;
    durationMinutes: number;
    price: number;
  }) => request<ServiceOffering>("/services", { method: "POST", body: input }),

  deactivateService: (id: string) => request<void>(`/services/${id}`, { method: "DELETE" }),

  slots: (serviceId: string, status?: SlotStatus, horizonDays = 90) => {
    const from = new Date().toISOString();
    const to = new Date(Date.now() + horizonDays * 24 * 60 * 60 * 1000).toISOString();
    const query = new URLSearchParams({ serviceId, from, to });
    if (status) {
      query.set("status", status);
    }
    return request<Slot[]>(`/slots?${query.toString()}`, { anonymous: true });
  },

  generateSlots: (
    serviceId: string,
    input: {
      fromDate: string;
      toDate: string;
      workDayStart: string;
      workDayEnd: string;
      skipWeekend: boolean;
      zoneId: string;
    },
  ) => request<Slot[]>(`/services/${serviceId}/slots/generate`, { method: "POST", body: input }),

  book: (input: { serviceId: string; slotId: string; note?: string; rewardId?: string }) =>
    request<Reservation>("/reservations", { method: "POST", body: input }),

  myReservations: () => request<PagedResponse<Reservation>>("/reservations/my"),

  providerReservations: () => request<PagedResponse<Reservation>>("/reservations/provider"),

  cancelReservation: (id: string) =>
    request<Reservation>(`/reservations/${id}/cancel`, { method: "POST" }),

  loyaltyRules: (providerId?: string) =>
    request<LoyaltyRule[]>(
      providerId ? `/loyalty/rules?providerId=${providerId}` : "/loyalty/rules",
      { anonymous: true },
    ),

  createLoyaltyRule: (input: {
    requiredAppointments: number;
    rewardDescription: string;
    rewardValidityDays: number;
  }) => request<LoyaltyRule>("/loyalty/rules", { method: "POST", body: input }),

  deactivateLoyaltyRule: (id: string) =>
    request<LoyaltyRule>(`/loyalty/rules/${id}`, { method: "DELETE" }),

  myLoyaltyAccounts: () => request<LoyaltyAccount[]>("/loyalty/accounts/my"),

  providerLoyaltyAccounts: () => request<LoyaltyAccount[]>("/loyalty/accounts/provider"),

  myRewards: () => request<LoyaltyReward[]>("/loyalty/rewards/my"),

  myNotifications: () => request<PagedResponse<Notification>>("/notifications/my"),

  templates: () => request<NotificationTemplate[]>("/notification-templates"),

  updateTemplate: (
    code: string,
    input: { description?: string; subject: string; body: string; active: boolean },
  ) => request<NotificationTemplate>(`/notification-templates/${code}`, {
    method: "PUT",
    body: input,
  }),
};
