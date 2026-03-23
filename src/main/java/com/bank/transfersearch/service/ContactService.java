package com.bank.transfersearch.service;

import com.bank.transfersearch.dto.ContactDTO;

import java.util.List;
import java.util.Set;

public interface ContactService {
    ContactDTO addContact(ContactDTO contactDTO);

    List<ContactDTO> searchContacts(Long userId, String keyword);

    Set<String> getRecentContacts(Long userId);

    void addRecentContact(Long userId, String contactName);
}
