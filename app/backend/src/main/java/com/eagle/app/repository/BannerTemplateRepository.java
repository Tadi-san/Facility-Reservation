package com.eagle.app.repository;

import com.eagle.app.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BannerTemplateRepository extends JpaRepository<BannerTemplate, Long> {
    Optional<BannerTemplate> findByTemplateKey(String templateKey);
    Optional<BannerTemplate> findByTemplateKeyAndActiveTrue(String templateKey);
}
