package com.bank.transfersearch.service;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.dto.SearchResponseDTO;


import java.util.List;
import java.util.Set;

public interface ContactService {
    ContactDTO addContact(ContactDTO contactDTO);

    SearchResponseDTO searchContacts(Long userId, String keyword);

    List<String> analyze(String text, String analyzer);

    List<String> suggestContacts(Long userId, String prefix);

    Set<String> getRecentContacts(Long userId);

    void addRecentContact(Long userId, String contactName);
}
