package com.eagle.app.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SensitiveDataConverter implements AttributeConverter<String, String> {
    private static SensitiveDataCrypto crypto;

    static void setCrypto(SensitiveDataCrypto cryptoInstance) {
        crypto = cryptoInstance;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (crypto == null) return attribute;
        return crypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (crypto == null) return dbData;
        return crypto.decrypt(dbData);
    }
}
