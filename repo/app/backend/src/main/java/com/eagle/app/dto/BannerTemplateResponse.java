package com.eagle.app.dto;

import com.eagle.app.model.BannerTemplate;

public record BannerTemplateResponse(Long id, String templateKey, Integer minutesBefore, String message, boolean active) {
    public static BannerTemplateResponse from(BannerTemplate b) {
        return new BannerTemplateResponse(b.id, b.templateKey, b.minutesBefore, b.message, b.active);
    }
}
