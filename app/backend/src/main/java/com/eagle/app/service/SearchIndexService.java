package com.eagle.app.service;

import com.eagle.app.model.SearchIndexDocument;
import com.eagle.app.repository.AnnouncementRepository;
import com.eagle.app.repository.PromotionRepository;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.RoomRepository;
import com.eagle.app.repository.SearchIndexDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SearchIndexService {
    private final SearchIndexDocumentRepository index;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final PromotionRepository promotions;
    private final AnnouncementRepository announcements;

    public SearchIndexService(SearchIndexDocumentRepository index,
                              RoomRepository rooms,
                              ReservationRepository reservations,
                              PromotionRepository promotions,
                              AnnouncementRepository announcements) {
        this.index = index;
        this.rooms = rooms;
        this.reservations = reservations;
        this.promotions = promotions;
        this.announcements = announcements;
    }

    @Transactional
    public int rebuildIndex() {
        index.deleteAllInBatch();
        int created = 0;
        for (var room : rooms.findAll()) {
            created += save("ROOM", room.id, room.number + " " + room.location.name + " " + room.roomType.name);
        }
        for (var reservation : reservations.findAll()) {
            created += save("RESERVATION", reservation.id, reservation.room.number + " " + reservation.requester.username + " " + reservation.status.name());
        }
        for (var promotion : promotions.findAll()) {
            created += save("PROMOTION", promotion.id, promotion.code + " " + promotion.promotionType);
        }
        for (var announcement : announcements.findAll()) {
            created += save("ANNOUNCEMENT", announcement.id, announcement.title + " " + announcement.message);
        }
        return created;
    }

    public List<Map<String, String>> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        return index.findTop50ByContentContainingIgnoreCaseOrderByUpdatedAtDesc(query.trim())
                .stream()
                .map(doc -> Map.of(
                        "sourceType", doc.sourceType,
                        "sourceId", doc.sourceId,
                        "content", doc.content))
                .toList();
    }

    private int save(String sourceType, Long sourceId, String content) {
        SearchIndexDocument doc = new SearchIndexDocument();
        doc.sourceType = sourceType;
        doc.sourceId = String.valueOf(sourceId);
        doc.content = content;
        index.save(doc);
        return 1;
    }
}
