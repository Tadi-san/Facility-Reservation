package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_index_documents")
public class SearchIndexDocument extends SyncableEntity {
    @Column(nullable = false)
    public String sourceType;

    @Column(nullable = false)
    public String sourceId;

    @Column(nullable = false, length = 4000)
    public String content;
}
