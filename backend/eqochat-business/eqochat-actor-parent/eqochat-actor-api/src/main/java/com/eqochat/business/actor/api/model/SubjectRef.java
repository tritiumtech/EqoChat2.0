package com.eqochat.business.actor.api.model;

import java.util.Objects;

public record SubjectRef(Long id, SubjectType type) {

    public SubjectRef {
        Objects.requireNonNull(type, "subject type must not be null");
    }

    public static SubjectRef human(Long id) {
        return new SubjectRef(id, SubjectType.HUMAN);
    }

    public static SubjectRef agent(Long id) {
        return new SubjectRef(id, SubjectType.AGENT);
    }

    public static SubjectRef system(Long id) {
        return new SubjectRef(id, SubjectType.SYSTEM);
    }

    public boolean isHuman() {
        return type == SubjectType.HUMAN;
    }

    public boolean isAgent() {
        return type == SubjectType.AGENT;
    }
}
