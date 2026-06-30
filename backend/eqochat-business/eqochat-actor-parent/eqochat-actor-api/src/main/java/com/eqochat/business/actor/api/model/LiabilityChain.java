package com.eqochat.business.actor.api.model;

public record LiabilityChain(SubjectRef actor, Long liableHumanId, String route, String reason) {

    public LiabilityChain(SubjectRef actor, Long liableHumanId, String route) {
        this(actor, liableHumanId, route, null);
    }

    public static LiabilityChain selfResponsible(Long humanId) {
        return new LiabilityChain(SubjectRef.human(humanId), humanId, "human:" + safe(humanId), null);
    }

    public static LiabilityChain agentToHuman(Long agentId, Long ownerId) {
        return new LiabilityChain(SubjectRef.agent(agentId), ownerId, "agent:" + safe(agentId) + "->human:" + safe(ownerId), null);
    }

    public static LiabilityChain unresolved(SubjectRef actor) {
        return unresolved(actor, "liability is unresolved");
    }

    public static LiabilityChain unresolved(SubjectRef actor, String reason) {
        String type = actor == null || actor.type() == null ? "unknown" : actor.type().jsonValue();
        Long id = actor == null ? null : actor.id();
        return new LiabilityChain(actor, null, type + ":" + safe(id) + "->human:unknown", reason);
    }

    private static String safe(Long id) {
        return id == null ? "unknown" : String.valueOf(id);
    }
}
