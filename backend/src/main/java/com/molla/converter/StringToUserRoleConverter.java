package com.molla.converter;

import com.molla.domain.UserRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToUserRoleConverter implements Converter<String, UserRole> {

    @Override
    public UserRole convert(String source) {
        try {
            return UserRole.fromString(source);
        } catch (IllegalArgumentException e) {
            // Returning null or throwing exception?
            // Better to return null if invalid so it can be handled or ignored,
            // but Spring usually expects valid conversion.
            // Let's rely on fromString's exception message which is good.
            throw e;
        }
    }
}
