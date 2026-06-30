package com.eqochat.business.contact.api.service;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.contact.api.dto.response.ContactDetailResponse;
import com.eqochat.business.contact.api.dto.response.ContactResponse;

import java.util.List;

public interface ContactService {

    List<ContactResponse> listContacts(Long principalHumanId, SubjectRef owner);

    /**
     * 当前用户与指定好友的详情（需为好友关系）。
     */
    ContactDetailResponse getContactDetail(Long principalHumanId, SubjectRef owner, SubjectRef target);

    ContactResponse addContact(Long principalHumanId, SubjectRef owner, SubjectRef target);

    List<String> updateContactTags(Long principalHumanId, SubjectRef owner, SubjectRef target, List<String> tags);
}
