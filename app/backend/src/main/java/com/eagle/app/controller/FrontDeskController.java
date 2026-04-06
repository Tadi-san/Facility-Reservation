package com.eagle.app.controller;

import com.eagle.app.dto.BannerTemplateRequest;
import com.eagle.app.dto.BannerTemplateResponse;
import com.eagle.app.dto.FrontDeskAgentResponse;
import com.eagle.app.dto.NotificationBannerResponse;
import com.eagle.app.dto.ReservationResponse;
import com.eagle.app.dto.ShiftHandoffRequest;
import com.eagle.app.dto.ShiftHandoffResponse;
import com.eagle.app.model.BannerTemplate;
import com.eagle.app.model.ReservationStatus;
import com.eagle.app.model.RoleName;
import com.eagle.app.model.ShiftHandoff;
import com.eagle.app.model.User;
import com.eagle.app.repository.BannerTemplateRepository;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.ShiftHandoffRepository;
import com.eagle.app.repository.UserRepository;
import com.eagle.app.service.AuditLogService;
import com.eagle.app.service.CurrentUserService;
import com.eagle.app.service.FrontDeskReservationService;
import com.eagle.app.service.NotificationJobService;
import com.eagle.app.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/frontdesk")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public class FrontDeskController {
    private final ReservationRepository reservations;
    private final BannerTemplateRepository templates;
    private final ShiftHandoffRepository handoffs;
    private final UserRepository users;
    private final FrontDeskReservationService reservationService;
    private final NotificationService notifications;
    private final NotificationJobService notificationJobService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public FrontDeskController(ReservationRepository reservations,
                               BannerTemplateRepository templates,
                               ShiftHandoffRepository handoffs,
                               UserRepository users,
                               FrontDeskReservationService reservationService,
                               NotificationService notifications,
                               NotificationJobService notificationJobService,
                               CurrentUserService currentUserService,
                               AuditLogService auditLogService) {
        this.reservations = reservations;
        this.templates = templates;
        this.handoffs = handoffs;
        this.users = users;
        this.reservationService = reservationService;
        this.notifications = notifications;
        this.notificationJobService = notificationJobService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/schedule-board")
    public Page<ReservationResponse> scheduleBoard(@RequestParam(required = false) String room,
                                                   @RequestParam(required = false) ReservationStatus status,
                                                   @RequestParam(required = false) Instant from,
                                                   @RequestParam(required = false) Instant to,
                                                   Pageable pageable) {
        List<ReservationResponse> list = reservations.findAll().stream().map(ReservationResponse::from)
                .filter(r -> room == null || room.isBlank() || r.roomNumber().equalsIgnoreCase(room))
                .filter(r -> status == null || r.status() == status)
                .filter(r -> from == null || !r.endTime().isBefore(from))
                .filter(r -> to == null || !r.startTime().isAfter(to))
                .sorted(Comparator.comparing(ReservationResponse::startTime))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return new PageImpl<>(start >= list.size() ? List.of() : list.subList(start, end), pageable, list.size());
    }

    @PostMapping("/reservations/{id}/confirm-arrival")
    public ReservationResponse confirmArrival(@PathVariable Long id) {
        var updated = reservationService.confirmArrival(id);
        auditLogService.log(currentUserService.requireCurrentUser(), "RESERVATION_CHECKIN", "Reservation", String.valueOf(id), "Confirmed arrival");
        return ReservationResponse.from(updated);
    }

    @PostMapping("/reservations/{id}/checkout")
    public ReservationResponse checkout(@PathVariable Long id) {
        var updated = reservationService.processCheckout(id);
        auditLogService.log(currentUserService.requireCurrentUser(), "RESERVATION_CHECKOUT", "Reservation", String.valueOf(id), "Completed checkout");
        return ReservationResponse.from(updated);
    }

    @GetMapping("/banner-templates")
    public Page<BannerTemplateResponse> bannerTemplates(Pageable pageable) {
        return templates.findAll(pageable).map(BannerTemplateResponse::from);
    }

    @PostMapping("/banner-templates")
    public BannerTemplate saveTemplate(@Valid @RequestBody BannerTemplateRequest req) {
        BannerTemplate t = templates.findByTemplateKey(req.templateKey()).orElseGet(BannerTemplate::new);
        t.templateKey = req.templateKey();
        t.minutesBefore = req.minutesBefore();
        t.message = req.message();
        t.active = req.active();
        BannerTemplate saved = templates.save(t);
        auditLogService.log(currentUserService.requireCurrentUser(), "BANNER_TEMPLATE_SAVED", "BannerTemplate", String.valueOf(saved.id), "Saved template " + saved.templateKey);
        return saved;
    }

    @GetMapping("/agents")
    public List<FrontDeskAgentResponse> agents() {
        return users.findByRoleNameOrderByUsernameAsc(RoleName.AGENT).stream().map(FrontDeskAgentResponse::from).toList();
    }

    @GetMapping("/shift-handoffs")
    public Page<ShiftHandoffResponse> shiftHandoffs(Pageable pageable) {
        return handoffs.findAll(pageable).map(ShiftHandoffResponse::from);
    }

    @PostMapping("/shift-handoffs")
    public ShiftHandoffResponse saveHandoff(@Valid @RequestBody ShiftHandoffRequest req) {
        ShiftHandoff h = new ShiftHandoff();
        h.fromUser = users.findById(req.fromUserId()).orElseThrow(() -> new IllegalArgumentException("from user not found"));
        h.toUser = users.findById(req.toUserId()).orElseThrow(() -> new IllegalArgumentException("to user not found"));
        h.handoffTime = req.handoffTime();
        h.summary = req.summary();
        h.pendingTasks = req.pendingTasks();
        ShiftHandoff saved = handoffs.save(h);
        auditLogService.log(currentUserService.requireCurrentUser(), "SHIFT_HANDOFF_SAVED", "ShiftHandoff", String.valueOf(saved.id), "Saved shift handoff");
        return ShiftHandoffResponse.from(saved);
    }

    @PostMapping("/notifications/refresh")
    public Map<String, Object> refresh() {
        User actor = currentUserService.requireCurrentUser();
        return notificationJobService.enqueueRefresh(actor);
    }

    @GetMapping("/notifications")
    public Page<NotificationBannerResponse> listNotifications(Pageable pageable) {
        List<NotificationBannerResponse> rows = notifications.activeAndHistoricalBanners().stream().map(NotificationBannerResponse::from).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        return new PageImpl<>(start >= rows.size() ? List.of() : rows.subList(start, end), pageable, rows.size());
    }

    @PostMapping("/notifications/{id}/dismiss")
    public NotificationBannerResponse dismiss(@PathVariable Long id) {
        var result = notifications.dismiss(id);
        auditLogService.log(currentUserService.requireCurrentUser(), "NOTIFICATION_DISMISSED", "NotificationBanner", String.valueOf(id), "Dismissed notification");
        return NotificationBannerResponse.from(result);
    }
}
