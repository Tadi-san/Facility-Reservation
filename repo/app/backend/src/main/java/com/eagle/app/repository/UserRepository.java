package com.eagle.app.repository;

import com.eagle.app.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRoleNameOrderByUsernameAsc(RoleName roleName);
    boolean existsByUsername(String username);
}
