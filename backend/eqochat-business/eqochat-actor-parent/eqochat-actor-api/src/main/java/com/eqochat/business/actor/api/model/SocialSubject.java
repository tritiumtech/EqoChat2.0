package com.eqochat.business.actor.api.model;

public interface SocialSubject {

    SubjectRef ref();

    String did();

    String displayName();

    String avatarUrl();

    SubjectStatus status();

    Integer points();

    Integer creditScore();

    CreditProfileSummary creditProfile();

    LiabilityChain liabilityChain();

    CapabilitySet capabilities();
}
