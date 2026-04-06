package com.eagle.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "attachment_moderation_items")
public class AttachmentModerationItem extends SyncableEntity {
    @Column(nullable = false)
    public String fileName;

    @Column(nullable = false)
    public String contentType;

    @Column(nullable = false)
    public String status;

    public String reason;

    @ManyToOne
    public User uploadedBy;
}
