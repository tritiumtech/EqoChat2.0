package com.eqochat.business.contact.api.service;

import com.eqochat.business.actor.api.model.SubjectRef;

import java.util.List;

public interface SubjectRelationshipApi {

    boolean areFriends(SubjectRef owner, SubjectRef target);

    List<SubjectRef> listFriends(SubjectRef owner);
}
