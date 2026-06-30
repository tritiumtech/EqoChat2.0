package com.eqochat.business.actor.api.model;

public record Capability(CapabilityCode code, CapabilityState state, String reason) {

    public static Capability enabled(CapabilityCode code) {
        return new Capability(code, CapabilityState.ENABLED, null);
    }

    public static Capability disabled(CapabilityCode code, String reason) {
        return new Capability(code, CapabilityState.DISABLED, reason);
    }

    public static Capability pending(CapabilityCode code, String reason) {
        return new Capability(code, CapabilityState.PENDING_APPROVAL, reason);
    }
}
