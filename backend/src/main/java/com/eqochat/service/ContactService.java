package com.eqochat.service;

import com.eqochat.dto.response.ContactDetailResponse;
import com.eqochat.dto.response.ContactResponse;

import java.util.List;

public interface ContactService {
    
    List<ContactResponse> listContacts(Long userId);

    /**
     * 当前用户与指定好友的详情（需为好友关系）。
     */
    ContactDetailResponse getContactDetail(Long userId, Long contactId);
    
    ContactResponse addContact(Long userId, Long friendId);
    
    List<String> updateContactTags(Long userId, Long friendId, List<String> tags);
}
