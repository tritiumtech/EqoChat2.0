package com.eqochat.business.actor.api.model;

import java.util.List;

public record CapabilitySet(List<Capability> capabilities) {

    public CapabilitySet {
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }

    public Capability get(CapabilityCode code) {
        if (code == null) {
            return null;
        }
        return capabilities.stream()
                .filter(item -> item != null && item.code() == code)
                .findFirst()
                .orElse(null);
    }

    public boolean enabled(CapabilityCode code) {
        Capability capability = get(code);
        return capability != null && capability.state() == CapabilityState.ENABLED;
    }
}
