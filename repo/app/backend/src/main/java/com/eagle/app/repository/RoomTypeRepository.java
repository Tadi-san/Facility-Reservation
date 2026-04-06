package com.eagle.app.repository;

import com.eagle.app.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    Optional<RoomType> findByName(String name);
}
