package com.eagle.app.controller;

import com.eagle.app.dto.AnnouncementRequest;
import com.eagle.app.dto.ModerationRequest;
import com.eagle.app.model.Announcement;
import com.eagle.app.model.AttachmentModerationItem;
import com.eagle.app.model.Promotion;
import com.eagle.app.repository.AnnouncementRepository;
import com.eagle.app.repository.AttachmentModerationItemRepository;
import com.eagle.app.repository.PromotionRepository;
import com.eagle.app.repository.UserRepository;
import com.eagle.app.service.AuditLogService;
import com.eagle.app.service.CurrentUserService;
import com.eagle.app.service.NotificationJobService;
import com.eagle.app.service.SearchIndexService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/operations")
@PreAuthorize("hasAnyRole('OPS','ADMIN')")
public class OperationsController {
    private final PromotionRepository promotions;
    private final AnnouncementRepository announcements;
    private final AttachmentModerationItemRepository moderation;
    private final UserRepository users;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final NotificationJobService notificationJobService;
    private final SearchIndexService searchIndexService;

    public OperationsController(PromotionRepository promotions,
                                AnnouncementRepository announcements,
                                AttachmentModerationItemRepository moderation,
                                UserRepository users,
                                CurrentUserService currentUserService,
                                AuditLogService auditLogService,
                                NotificationJobService notificationJobService,
                                SearchIndexService searchIndexService) {
        this.promotions = promotions;
        this.announcements = announcements;
        this.moderation = moderation;
        this.users = users;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.notificationJobService = notificationJobService;
        this.searchIndexService = searchIndexService;
    }

    @GetMapping("/promotions")
    public Page<Promotion> listPromotions(Pageable pageable) {
        return promotions.findAll(pageable);
    }

    @PostMapping("/promotions")
    public Promotion savePromotion(@Valid @RequestBody Promotion p) {
        Promotion saved = promotions.save(p);
        auditLogService.log(currentUserService.requireCurrentUser(), "PROMOTION_SAVED", "Promotion", String.valueOf(saved.id), "Saved promotion " + saved.code);
        return saved;
    }

    @GetMapping("/announcements")
    public Page<Announcement> listAnnouncements(Pageable pageable) {
        return announcements.findAll(pageable);
    }

    @PostMapping("/announcements")
    public Announcement saveAnnouncement(@Valid @RequestBody AnnouncementRequest req) {
        Announcement a = new Announcement();
        a.title = req.title();
        a.message = req.message();
        a.published = req.published();
        Announcement saved = announcements.save(a);
        auditLogService.log(currentUserService.requireCurrentUser(), "ANNOUNCEMENT_SAVED", "Announcement", String.valueOf(saved.id), "Saved announcement");
        return saved;
    }

    @GetMapping("/moderation-queue")
    public Page<AttachmentModerationItem> listModeration(Pageable pageable) {
        return moderation.findAll(pageable);
    }

    @PostMapping("/moderation-queue")
    public AttachmentModerationItem saveModeration(@Valid @RequestBody ModerationRequest req) {
        AttachmentModerationItem i = new AttachmentModerationItem();
        i.fileName = req.fileName();
        i.contentType = req.contentType();
        i.status = req.status();
        i.reason = req.reason();
        if (req.uploadedById() != null) {
            i.uploadedBy = users.findById(req.uploadedById()).orElse(null);
        }
        AttachmentModerationItem saved = moderation.save(i);
        auditLogService.log(currentUserService.requireCurrentUser(), "MODERATION_ITEM_SAVED", "AttachmentModerationItem", String.valueOf(saved.id), "Saved moderation item");
        return saved;
    }

    @PostMapping("/search/reindex")
    public Map<String, Object> queueSearchReindex() {
        return notificationJobService.enqueueSearchReindex(currentUserService.requireCurrentUser());
    }

    @GetMapping("/search")
    public List<Map<String, String>> search(@RequestParam String q) {
        return searchIndexService.search(q);
    }
}
