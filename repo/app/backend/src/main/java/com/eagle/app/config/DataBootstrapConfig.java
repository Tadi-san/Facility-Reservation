package com.eagle.app.config;

import com.eagle.app.model.*;
import com.eagle.app.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;

@Configuration
public class DataBootstrapConfig {

    @Bean
    CommandLineRunner seed(UserRepository users,
                           LocationRepository locations,
                           RoomTypeRepository roomTypes,
                           RoomRepository rooms,
                           AssetRepository assets,
                           PromotionRepository promotions,
                           BannerTemplateRepository templates,
                           PasswordEncoder encoder) {
        return args -> {
            createUser(users, encoder, "requester.demo", RoleName.REQUESTER);
            createUser(users, encoder, "agent.demo", RoleName.AGENT);
            createUser(users, encoder, "tech.demo", RoleName.TECH);
            createUser(users, encoder, "ops.demo", RoleName.OPS);
            createUser(users, encoder, "admin.demo", RoleName.ADMIN);

            Location hq = locations.findByCode("HQ").orElseGet(() -> {
                Location l = new Location();
                l.code = "HQ";
                l.name = "Main Campus";
                l.address = "100 Enterprise Way";
                return locations.save(l);
            });

            RoomType board = roomTypes.findByName("Boardroom").orElseGet(() -> {
                RoomType t = new RoomType();
                t.name = "Boardroom";
                t.description = "Executive room";
                return roomTypes.save(t);
            });

            Room a101 = rooms.findAll().stream().filter(r -> "A101".equals(r.number)).findFirst().orElseGet(() -> {
                Room r = new Room();
                r.location = hq;
                r.roomType = board;
                r.number = "A101";
                r.capacity = 14;
                r.floorNumber = 1;
                return rooms.save(r);
            });

            Room a102 = rooms.findAll().stream().filter(r -> "A102".equals(r.number)).findFirst().orElseGet(() -> {
                Room r = new Room();
                r.location = hq;
                r.roomType = board;
                r.number = "A102";
                r.capacity = 12;
                r.floorNumber = 1;
                return rooms.save(r);
            });

            createAsset(assets, a101, "PROJ-A101", AssetType.PROJECTOR);
            createAsset(assets, a101, "HVAC-A101", AssetType.HVAC);
            createAsset(assets, a102, "PROJ-A102", AssetType.PROJECTOR);
            createAsset(assets, a102, "HVAC-A102", AssetType.HVAC);

            if (promotions.findByCode("EARLY10").isEmpty()) {
                Promotion p = new Promotion();
                p.code = "EARLY10";
                p.percentage = BigDecimal.TEN;
                p.promotionType = "EARLY_BIRD";
                p.startsAt = Instant.now().minusSeconds(86400);
                p.endsAt = Instant.now().plusSeconds(86400 * 30L);
                p.active = true;
                promotions.save(p);
            }

            createTemplate(templates, "ARRIVAL_30M", 30, "Reminder: reservation starts in 30 minutes.");
            createTemplate(templates, "CHECKOUT_10M", 10, "Reminder: checkout is in 10 minutes.");
        };
    }

    private void createUser(UserRepository repo, PasswordEncoder encoder, String username, RoleName role) {
        if (repo.existsByUsername(username)) return;
        User u = new User();
        u.username = username;
        u.email = username + "@eagle.local";
        u.passwordHash = encoder.encode("ChangeMe!1234");
        u.roleName = role;
        u.staffContactInfo = "+1-555-0100";
        repo.save(u);
    }

    private void createAsset(AssetRepository repo, Room room, String tag, AssetType type) {
        if (repo.existsByTag(tag)) return;
        Asset a = new Asset();
        a.room = room;
        a.tag = tag;
        a.name = type.name() + " " + room.number;
        a.assetType = type;
        repo.save(a);
    }

    private void createTemplate(BannerTemplateRepository repo, String key, int min, String msg) {
        if (repo.findByTemplateKey(key).isPresent()) return;
        BannerTemplate t = new BannerTemplate();
        t.templateKey = key;
        t.minutesBefore = min;
        t.message = msg;
        t.active = true;
        repo.save(t);
    }
}
