package com.eqochat.service;

import com.eqochat.dto.response.ContactResponse;

import java.util.List;

public interface ContactService {
    
    List<ContactResponse> listContacts(Long userId);
    
    ContactResponse addContact(Long userId, Long friendId);

    List<String> updateContactTags(Long userId, Long friendId, List<String> tags);
}
