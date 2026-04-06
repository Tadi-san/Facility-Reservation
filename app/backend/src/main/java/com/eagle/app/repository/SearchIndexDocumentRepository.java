package com.eagle.app.repository;

import com.eagle.app.model.SearchIndexDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchIndexDocumentRepository extends JpaRepository<SearchIndexDocument, Long> {
    List<SearchIndexDocument> findTop50ByContentContainingIgnoreCaseOrderByUpdatedAtDesc(String query);
}
