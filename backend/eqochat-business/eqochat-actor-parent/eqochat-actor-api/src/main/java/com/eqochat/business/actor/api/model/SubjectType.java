package com.eqochat.business.actor.api.model;

import java.util.Locale;
import java.util.Objects;

public enum SubjectType {
    HUMAN,
    AGENT,
    SYSTEM;

    public static SubjectType from(String raw) {
        String v = normalize(raw);
        return SubjectType.valueOf(v);
    }

    public String jsonValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String raw) {
        String v = Objects.requireNonNull(raw, "subject type must not be null").trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("subject type must not be blank");
        }
        return v.toUpperCase(Locale.ROOT);
    }
}
