import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import {
  ApiService,
  BannerTemplate,
  FrontDeskAgent,
  MaintenanceTicket,
  ModerationItem,
  NotificationBanner,
  Reservation,
  Room,
  ShiftHandoff
} from "./api.service";

type Workspace = "requester" | "frontdesk" | "maintenance" | "operations" | "admin";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: "./app.component.html",
  styleUrl: "./app.component.css"
})
export class AppComponent {
  rooms: Room[] = [];
  myReservations: Reservation[] = [];
  scheduleBoard: Reservation[] = [];
  notifications: NotificationBanner[] = [];
  bannerTemplates: BannerTemplate[] = [];
  shiftHandoffs: ShiftHandoff[] = [];
  frontDeskAgents: FrontDeskAgent[] = [];
  maintenanceTickets: MaintenanceTicket[] = [];
  inspections: Array<{ roomNumber: string; inspectionTime: string; outcome: string }> = [];
  promotions: Array<{ code: string; percentage: number }> = [];
  announcements: Array<{ title: string; message: string }> = [];
  moderationItems: ModerationItem[] = [];
  searchResults: Array<{ sourceType: string; sourceId: string; content: string }> = [];
  users: Array<{ id: number; username: string; email: string }> = [];
  scorecards: Array<{ metricKey: string; version: number; effectiveFrom: string }> = [];
  roles: string[] = [];
  status = "Sign in to load workspaces.";
  activeWorkspace: Workspace = "requester";

  readonly loginForm = this.fb.nonNullable.group({
    username: ["requester.demo", [Validators.required]],
    password: ["ChangeMe!1234", [Validators.required]]
  });
  readonly reservationForm = this.fb.nonNullable.group({
    roomId: [0, [Validators.min(1)]],
    startTime: ["", [Validators.required]],
    endTime: ["", [Validators.required]]
  });
  readonly bannerTemplateForm = this.fb.nonNullable.group({
    templateKey: ["ARRIVAL_15M", [Validators.required]],
    minutesBefore: [15, [Validators.required]],
    message: ["Arrival reminder in 15 minutes.", [Validators.required]]
  });
  readonly shiftHandoffForm = this.fb.nonNullable.group({
    fromUserId: [0, [Validators.required]],
    toUserId: [0, [Validators.required]],
    summary: ["Desk handoff summary", [Validators.required]],
    pendingTasks: ["Check late arrivals"]
  });
  readonly maintenanceForm = this.fb.nonNullable.group({
    title: ["HVAC incident", [Validators.required]],
    roomNumber: ["A101", [Validators.required]],
    description: ["Temperature instability", [Validators.required]]
  });
  readonly promotionForm = this.fb.nonNullable.group({
    code: ["EARLY15", [Validators.required]],
    percentage: [15, [Validators.min(1), Validators.max(100)]]
  });
  readonly announcementForm = this.fb.nonNullable.group({
    title: ["Daily Operations Update", [Validators.required]],
    message: ["Please review priority tickets.", [Validators.required]]
  });
  readonly moderationForm = this.fb.nonNullable.group({
    fileName: ["lobby-cam-1.jpg", [Validators.required]],
    contentType: ["image/jpeg", [Validators.required]],
    status: ["PENDING", [Validators.required]],
    reason: ["Awaiting review"]
  });
  readonly searchForm = this.fb.nonNullable.group({
    query: ["A101", [Validators.required]]
  });
  readonly userForm = this.fb.nonNullable.group({
    username: ["angular.user", [Validators.required]],
    email: ["angular.user@eagle.local", [Validators.required]],
    password: ["Complex#Pass123", [Validators.required]],
    role: ["REQUESTER", [Validators.required]]
  });
  readonly scorecardForm = this.fb.nonNullable.group({
    metricKey: ["ops_scorecard", [Validators.required]]
  });

  workspaceTabs: Array<{ id: Workspace; title: string; role: string }> = [
    { id: "requester", title: "Requester", role: "REQUESTER" },
    { id: "frontdesk", title: "Front Desk", role: "AGENT" },
    { id: "maintenance", title: "Maintenance", role: "TECH" },
    { id: "operations", title: "Operations", role: "OPS" },
    { id: "admin", title: "Admin", role: "ADMIN" }
  ];

  constructor(private fb: FormBuilder, private api: ApiService) {}

  signIn(): void {
    if (this.loginForm.invalid) return;
    const value = this.loginForm.getRawValue();
    this.api.login(value.username, value.password).subscribe({
      next: (response) => {
        localStorage.setItem("eagle_token", response.token);
        this.roles = this.extractRoles(response.token);
        this.status = "Authenticated. Loading role-based workspaces...";
        this.activeWorkspace = this.defaultWorkspace();
        this.loadAllWorkspaces();
      },
      error: (error) => (this.status = error?.error?.message ?? "Login failed.")
    });
  }

  switchWorkspace(workspace: Workspace): void {
    if (!this.isWorkspaceVisible(workspace)) return;
    this.activeWorkspace = workspace;
  }

  isWorkspaceVisible(workspace: Workspace): boolean {
    const tab = this.workspaceTabs.find((x) => x.id === workspace);
    if (!tab) return false;
    return this.roles.includes(tab.role) || this.roles.includes("ADMIN");
  }

  loadAllWorkspaces(): void {
    this.loadRequester();
    if (this.isWorkspaceVisible("frontdesk")) this.loadFrontdesk();
    if (this.isWorkspaceVisible("maintenance")) this.loadMaintenance();
    if (this.isWorkspaceVisible("operations")) this.loadOperations();
    if (this.isWorkspaceVisible("admin")) this.loadAdmin();
  }

  loadRequester(): void {
    this.api.rooms().subscribe({
      next: (r) => {
        this.rooms = r.content;
        if (this.rooms.length) this.reservationForm.patchValue({ roomId: this.rooms[0].id });
      }
    });
    this.api.myReservations().subscribe({ next: (r) => (this.myReservations = r.content) });
  }

  loadFrontdesk(): void {
    this.api.scheduleBoard().subscribe({ next: (r) => (this.scheduleBoard = r.content) });
    this.api.listNotifications().subscribe({ next: (r) => (this.notifications = r.content) });
    this.api.listBannerTemplates().subscribe({ next: (r) => (this.bannerTemplates = r.content) });
    this.api.listShiftHandoffs().subscribe({ next: (r) => (this.shiftHandoffs = r.content) });
    this.api.listFrontDeskAgents().subscribe({
      next: (rows) => {
        this.frontDeskAgents = rows;
        if (rows.length > 1) {
          this.shiftHandoffForm.patchValue({ fromUserId: rows[0].id, toUserId: rows[1].id });
        } else if (rows.length === 1) {
          this.shiftHandoffForm.patchValue({ fromUserId: rows[0].id, toUserId: rows[0].id });
        }
      }
    });
  }

  loadMaintenance(): void {
    this.api.listMaintenanceTickets().subscribe({ next: (r) => (this.maintenanceTickets = r.content) });
    this.api.listInspections().subscribe({ next: (r) => (this.inspections = r.content) });
  }

  loadOperations(): void {
    this.api.listPromotions().subscribe({ next: (r) => (this.promotions = r.content) });
    this.api.listAnnouncements().subscribe({ next: (r) => (this.announcements = r.content) });
    this.api.listModerationQueue().subscribe({ next: (r) => (this.moderationItems = r.content) });
  }

  loadAdmin(): void {
    this.api.users().subscribe({ next: (r) => (this.users = r.content) });
    this.api.scorecards().subscribe({ next: (r) => (this.scorecards = r.content) });
  }

  submitReservation(): void {
    if (this.reservationForm.invalid) return;
    const v = this.reservationForm.getRawValue();
    this.api.createReservation(v.roomId, new Date(v.startTime).toISOString(), new Date(v.endTime).toISOString()).subscribe({
      next: () => {
        this.status = "Requester flow: reservation created.";
        this.loadRequester();
      },
      error: (error) => (this.status = error?.error?.message ?? "Reservation failed.")
    });
  }

  confirmArrival(id: number): void {
    this.api.confirmArrival(id).subscribe({ next: () => { this.status = "Front desk flow: arrival confirmed."; this.loadFrontdesk(); } });
  }

  checkout(id: number): void {
    this.api.checkout(id).subscribe({ next: () => { this.status = "Front desk flow: checkout completed."; this.loadFrontdesk(); } });
  }

  runNotificationQueue(): void {
    this.api.refreshNotifications().subscribe({ next: () => (this.status = "Front desk flow: notification refresh queued.") });
  }

  dismissNotification(id: number): void {
    this.api.dismissNotification(id).subscribe({ next: () => { this.status = "Front desk flow: notification dismissed."; this.loadFrontdesk(); } });
  }

  saveBannerTemplate(): void {
    if (this.bannerTemplateForm.invalid) return;
    const v = this.bannerTemplateForm.getRawValue();
    this.api.saveBannerTemplate(v.templateKey, Number(v.minutesBefore), v.message, true).subscribe({
      next: () => {
        this.status = "Front desk flow: banner template saved.";
        this.loadFrontdesk();
      }
    });
  }

  saveShiftHandoff(): void {
    if (this.shiftHandoffForm.invalid) return;
    const v = this.shiftHandoffForm.getRawValue();
    this.api.createShiftHandoff(Number(v.fromUserId), Number(v.toUserId), v.summary, v.pendingTasks ?? "").subscribe({
      next: () => {
        this.status = "Front desk flow: shift handoff saved.";
        this.loadFrontdesk();
      }
    });
  }

  openMaintenanceTicket(): void {
    if (this.maintenanceForm.invalid) return;
    const v = this.maintenanceForm.getRawValue();
    this.api.openMaintenanceTicket(v.title, v.roomNumber, v.description).subscribe({
      next: () => {
        this.status = "Maintenance flow: ticket opened.";
        this.loadMaintenance();
      },
      error: (error) => (this.status = error?.error?.message ?? "Could not open ticket.")
    });
  }

  closeTicket(id: number): void {
    this.api.closeMaintenanceTicket(id, "Closed from Angular maintenance workspace").subscribe({
      next: () => {
        this.status = "Maintenance flow: ticket closed.";
        this.loadMaintenance();
      }
    });
  }

  createPromotion(): void {
    if (this.promotionForm.invalid) return;
    const v = this.promotionForm.getRawValue();
    this.api.createPromotion(v.code, Number(v.percentage)).subscribe({
      next: () => {
        this.status = "Operations flow: promotion created.";
        this.loadOperations();
      }
    });
  }

  createAnnouncement(): void {
    if (this.announcementForm.invalid) return;
    const v = this.announcementForm.getRawValue();
    this.api.createAnnouncement(v.title, v.message).subscribe({
      next: () => {
        this.status = "Operations flow: announcement published.";
        this.loadOperations();
      }
    });
  }

  addModerationItem(): void {
    if (this.moderationForm.invalid) return;
    const v = this.moderationForm.getRawValue();
    this.api.addModerationItem(v.fileName, v.contentType, v.status, v.reason).subscribe({
      next: () => {
        this.status = "Operations flow: moderation item added.";
        this.loadOperations();
      }
    });
  }

  queueReindex(): void {
    this.api.queueSearchReindex().subscribe({ next: () => (this.status = "Operations flow: search reindex queued.") });
  }

  runSearch(): void {
    if (this.searchForm.invalid) return;
    this.api.searchOperations(this.searchForm.getRawValue().query).subscribe({ next: (rows) => (this.searchResults = rows) });
  }

  createUser(): void {
    if (this.userForm.invalid) return;
    const v = this.userForm.getRawValue();
    this.api.createUser(v.username, v.email, v.password, v.role).subscribe({
      next: () => {
        this.status = "Admin flow: user created.";
        this.loadAdmin();
      }
    });
  }

  createScorecard(): void {
    if (this.scorecardForm.invalid) return;
    const v = this.scorecardForm.getRawValue();
    this.api.createScorecard(v.metricKey).subscribe({
      next: () => {
        this.status = "Admin flow: scorecard created.";
        this.loadAdmin();
      }
    });
  }

  private extractRoles(token: string): string[] {
    try {
      const encoded = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
      const padded = encoded.padEnd(Math.ceil(encoded.length / 4) * 4, "=");
      const payload = JSON.parse(atob(padded));
      if (!Array.isArray(payload.roles)) return [];
      return payload.roles.map((x: string) => x.replace("ROLE_", ""));
    } catch {
      return [];
    }
  }

  private defaultWorkspace(): Workspace {
    const first = this.workspaceTabs.find((tab) => this.roles.includes(tab.role) || this.roles.includes("ADMIN"));
    return first?.id ?? "requester";
  }
}
