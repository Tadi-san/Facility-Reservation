import { TestBed } from "@angular/core/testing";
import { of } from "rxjs";
import { AppComponent } from "./app.component";
import { ApiService } from "./api.service";

describe("AppComponent", () => {
  const apiMock = {
    login: jasmine.createSpy("login").and.returnValue(of({ token: "x.eyJyb2xlcyI6WyJST0xFX0FETUlOIl19.y" })),
    rooms: jasmine.createSpy("rooms").and.returnValue(of({ content: [] })),
    myReservations: jasmine.createSpy("myReservations").and.returnValue(of({ content: [] })),
    scheduleBoard: jasmine.createSpy("scheduleBoard").and.returnValue(of({ content: [] })),
    listNotifications: jasmine.createSpy("listNotifications").and.returnValue(of({ content: [] })),
    listBannerTemplates: jasmine.createSpy("listBannerTemplates").and.returnValue(of({ content: [] })),
    listShiftHandoffs: jasmine.createSpy("listShiftHandoffs").and.returnValue(of({ content: [] })),
    listFrontDeskAgents: jasmine.createSpy("listFrontDeskAgents").and.returnValue(of([])),
    listMaintenanceTickets: jasmine.createSpy("listMaintenanceTickets").and.returnValue(of({ content: [] })),
    listInspections: jasmine.createSpy("listInspections").and.returnValue(of({ content: [] })),
    listPromotions: jasmine.createSpy("listPromotions").and.returnValue(of({ content: [] })),
    listAnnouncements: jasmine.createSpy("listAnnouncements").and.returnValue(of({ content: [] })),
    listModerationQueue: jasmine.createSpy("listModerationQueue").and.returnValue(of({ content: [] })),
    users: jasmine.createSpy("users").and.returnValue(of({ content: [] })),
    scorecards: jasmine.createSpy("scorecards").and.returnValue(of({ content: [] })),
    createReservation: jasmine.createSpy("createReservation").and.returnValue(of({})),
    saveBannerTemplate: jasmine.createSpy("saveBannerTemplate").and.returnValue(of({})),
    createShiftHandoff: jasmine.createSpy("createShiftHandoff").and.returnValue(of({})),
    addModerationItem: jasmine.createSpy("addModerationItem").and.returnValue(of({})),
    confirmArrival: jasmine.createSpy("confirmArrival").and.returnValue(of({})),
    checkout: jasmine.createSpy("checkout").and.returnValue(of({})),
    refreshNotifications: jasmine.createSpy("refreshNotifications").and.returnValue(of({})),
    dismissNotification: jasmine.createSpy("dismissNotification").and.returnValue(of({})),
    openMaintenanceTicket: jasmine.createSpy("openMaintenanceTicket").and.returnValue(of({})),
    closeMaintenanceTicket: jasmine.createSpy("closeMaintenanceTicket").and.returnValue(of({})),
    createPromotion: jasmine.createSpy("createPromotion").and.returnValue(of({})),
    createAnnouncement: jasmine.createSpy("createAnnouncement").and.returnValue(of({})),
    queueSearchReindex: jasmine.createSpy("queueSearchReindex").and.returnValue(of({})),
    searchOperations: jasmine.createSpy("searchOperations").and.returnValue(of([])),
    createUser: jasmine.createSpy("createUser").and.returnValue(of({})),
    createScorecard: jasmine.createSpy("createScorecard").and.returnValue(of({}))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: ApiService, useValue: apiMock }]
    }).compileComponents();
  });

  it("loads workspaces after sign in", () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    component.signIn();
    expect(apiMock.rooms).toHaveBeenCalled();
    expect(apiMock.listBannerTemplates).toHaveBeenCalled();
    expect(apiMock.listModerationQueue).toHaveBeenCalled();
  });

  it("submits banner template flow", () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    component.saveBannerTemplate();
    expect(apiMock.saveBannerTemplate).toHaveBeenCalled();
  });

  it("submits moderation queue flow", () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    component.addModerationItem();
    expect(apiMock.addModerationItem).toHaveBeenCalled();
  });

  it("shows frontdesk workspace when role is AGENT", () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    component.roles = ["AGENT"];
    expect(component.isWorkspaceVisible("frontdesk")).toBeTrue();
    expect(component.isWorkspaceVisible("operations")).toBeFalse();
  });

  it("submits shift handoff flow", () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    component.shiftHandoffForm.patchValue({ fromUserId: 1, toUserId: 2 });
    component.saveShiftHandoff();
    expect(apiMock.createShiftHandoff).toHaveBeenCalled();
  });
});
