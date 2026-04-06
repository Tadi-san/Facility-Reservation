package com.eagle.app.repository;

import com.eagle.app.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCode(String code);
    Optional<Location> findByName(String name);
}
