import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

export interface AuthResponse {
  token: string;
}

export interface PageResponse<T> {
  content: T[];
}

export interface Room {
  id: number;
  roomNumber: string;
  location: string;
  roomType: string;
  capacity: number;
  available: boolean;
  availabilityMessage: string;
}

export interface Reservation {
  id: number;
  roomNumber: string;
  requesterUsername: string;
  startTime: string;
  endTime: string;
  status: string;
}

export interface NotificationBanner {
  id: number;
  roomNumber: string;
  requesterUsername: string;
  templateKey: string;
  message: string;
  status: string;
}

export interface BannerTemplate {
  id: number;
  templateKey: string;
  minutesBefore: number;
  message: string;
  active: boolean;
}

export interface FrontDeskAgent {
  id: number;
  username: string;
}

export interface ShiftHandoff {
  id: number;
  fromUser: string;
  toUser: string;
  handoffTime: string;
  summary: string;
  pendingTasks: string;
}

export interface MaintenanceTicket {
  id: number;
  title: string;
  roomNumber: string;
  status: string;
  overdue: boolean;
  slaDueAt: string;
}

export interface ModerationItem {
  id: number;
  fileName: string;
  contentType: string;
  status: string;
  reason: string;
}

@Injectable({ providedIn: "root" })
export class ApiService {
  private readonly baseUrl = "http://localhost:8080";

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.url("/api/v1/auth/login"), { username, password });
  }

  rooms(): Observable<PageResponse<Room>> {
    return this.http.get<PageResponse<Room>>(this.url("/api/v1/catalog/rooms?page=0&size=20"));
  }

  myReservations(): Observable<PageResponse<Reservation>> {
    return this.http.get<PageResponse<Reservation>>(this.url("/api/v1/reservations/mine?page=0&size=20"));
  }

  scheduleBoard(): Observable<PageResponse<Reservation>> {
    return this.http.get<PageResponse<Reservation>>(this.url("/api/v1/frontdesk/schedule-board?page=0&size=20"));
  }

  createReservation(roomId: number, startTime: string, endTime: string): Observable<unknown> {
    return this.http.post(this.url("/api/v1/reservations"), { roomId, startTime, endTime });
  }

  confirmArrival(id: number): Observable<unknown> {
    return this.http.post(this.url(`/api/v1/frontdesk/reservations/${id}/confirm-arrival`), {});
  }

  checkout(id: number): Observable<unknown> {
    return this.http.post(this.url(`/api/v1/frontdesk/reservations/${id}/checkout`), {});
  }

  refreshNotifications(): Observable<unknown> {
    return this.http.post(this.url("/api/v1/frontdesk/notifications/refresh"), {});
  }

  listNotifications(): Observable<PageResponse<NotificationBanner>> {
    return this.http.get<PageResponse<NotificationBanner>>(this.url("/api/v1/frontdesk/notifications?page=0&size=20"));
  }

  dismissNotification(id: number): Observable<unknown> {
    return this.http.post(this.url(`/api/v1/frontdesk/notifications/${id}/dismiss`), {});
  }

  listBannerTemplates(): Observable<PageResponse<BannerTemplate>> {
    return this.http.get<PageResponse<BannerTemplate>>(this.url("/api/v1/frontdesk/banner-templates?page=0&size=20"));
  }

  saveBannerTemplate(templateKey: string, minutesBefore: number, message: string, active: boolean): Observable<unknown> {
    return this.http.post(this.url("/api/v1/frontdesk/banner-templates"), { templateKey, minutesBefore, message, active });
  }

  listFrontDeskAgents(): Observable<FrontDeskAgent[]> {
    return this.http.get<FrontDeskAgent[]>(this.url("/api/v1/frontdesk/agents"));
  }

  listShiftHandoffs(): Observable<PageResponse<ShiftHandoff>> {
    return this.http.get<PageResponse<ShiftHandoff>>(this.url("/api/v1/frontdesk/shift-handoffs?page=0&size=20"));
  }

  createShiftHandoff(fromUserId: number, toUserId: number, summary: string, pendingTasks: string): Observable<unknown> {
    return this.http.post(this.url("/api/v1/frontdesk/shift-handoffs"), {
      fromUserId,
      toUserId,
      handoffTime: new Date().toISOString(),
      summary,
      pendingTasks
    });
  }

  listMaintenanceTickets(): Observable<PageResponse<MaintenanceTicket>> {
    return this.http.get<PageResponse<MaintenanceTicket>>(this.url("/api/v1/maintenance/tickets?page=0&size=20"));
  }

  listInspections(): Observable<PageResponse<{ roomNumber: string; inspectionTime: string; outcome: string }>> {
    return this.http.get<PageResponse<{ roomNumber: string; inspectionTime: string; outcome: string }>>(this.url("/api/v1/maintenance/inspections?page=0&size=20"));
  }

  openMaintenanceTicket(title: string, roomNumber: string, description: string): Observable<unknown> {
    return this.http.post(this.url("/api/v1/maintenance/tickets"), { title, roomNumber, description });
  }

  closeMaintenanceTicket(id: number, closureOutcome: string): Observable<unknown> {
    return this.http.post(this.url(`/api/v1/maintenance/tickets/${id}/close`), { closureOutcome });
  }

  listPromotions(): Observable<PageResponse<{ code: string; percentage: number }>> {
    return this.http.get<PageResponse<{ code: string; percentage: number }>>(this.url("/api/v1/operations/promotions?page=0&size=20"));
  }

  createPromotion(code: string, percentage: number): Observable<unknown> {
    const now = new Date();
    const end = new Date(now.getTime() + 86400000 * 30);
    return this.http.post(this.url("/api/v1/operations/promotions"), {
      code,
      percentage,
      promotionType: "EARLY_BIRD",
      startsAt: now.toISOString(),
      endsAt: end.toISOString(),
      active: true
    });
  }

  listAnnouncements(): Observable<PageResponse<{ title: string; message: string }>> {
    return this.http.get<PageResponse<{ title: string; message: string }>>(this.url("/api/v1/operations/announcements?page=0&size=20"));
  }

  createAnnouncement(title: string, message: string): Observable<unknown> {
    return this.http.post(this.url("/api/v1/operations/announcements"), { title, message, published: true });
  }

  listModerationQueue(): Observable<PageResponse<ModerationItem>> {
    return this.http.get<PageResponse<ModerationItem>>(this.url("/api/v1/operations/moderation-queue?page=0&size=20"));
  }

  addModerationItem(fileName: string, contentType: string, status: string, reason: string): Observable<unknown> {
    return this.http.post(this.url("/api/v1/operations/moderation-queue"), { fileName, contentType, status, reason });
  }

  queueSearchReindex(): Observable<unknown> {
    return this.http.post(this.url("/api/v1/operations/search/reindex"), {});
  }

  searchOperations(query: string): Observable<Array<{ sourceType: string; sourceId: string; content: string }>> {
    return this.http.get<Array<{ sourceType: string; sourceId: string; content: string }>>(this.url(`/api/v1/operations/search?q=${encodeURIComponent(query)}`));
  }

  users(): Observable<PageResponse<{ id: number; username: string; email: string }>> {
    return this.http.get<PageResponse<{ id: number; username: string; email: string }>>(this.url("/api/v1/users?page=0&size=20"));
  }

  createUser(username: string, email: string, password: string, role: string): Observable<unknown> {
    return this.http.post(this.url("/api/v1/users"), { username, email, password, role, staffContactInfo: "+1-555-0100" });
  }

  scorecards(): Observable<PageResponse<{ metricKey: string; version: number; effectiveFrom: string }>> {
    return this.http.get<PageResponse<{ metricKey: string; version: number; effectiveFrom: string }>>(this.url("/api/v1/metrics/scorecards?page=0&size=20"));
  }

  createScorecard(metricKey: string): Observable<unknown> {
    return this.http.post(this.url("/api/v1/metrics/scorecards"), {
      metricKey,
      effectiveFrom: "04/05/2026",
      definition: "{\"quality\":40,\"speed\":30,\"compliance\":30}",
      weightedDimensions: { quality: 40, speed: 30, compliance: 30 }
    });
  }

  private url(path: string): string {
    return `${this.baseUrl}${path}`;
  }
}
