package com.eagle.app.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SensitiveDataConfig {
    private final SensitiveDataCrypto crypto;

    public SensitiveDataConfig(SensitiveDataCrypto crypto) {
        this.crypto = crypto;
    }

    @PostConstruct
    void wireConverter() {
        SensitiveDataConverter.setCrypto(crypto);
    }
}
