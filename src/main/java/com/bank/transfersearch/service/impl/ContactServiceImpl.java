package com.bank.transfersearch.service.impl;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.entity.Contact;
import com.bank.transfersearch.entity.ContactDocument;
import com.bank.transfersearch.kafka.ContactProducer;
import com.bank.transfersearch.mapper.ContactMapper;
import com.bank.transfersearch.repository.ContactRepository;
import com.bank.transfersearch.search.ContactSearchRepository;
import com.bank.transfersearch.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final ContactSearchRepository contactSearchRepository;
    private final ContactMapper contactMapper;
    private final ContactProducer contactProducer;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String RECENT_CONTACTS_KEY_PREFIX = "recent:contacts:";

    @Override
    @Transactional
    public ContactDTO addContact(ContactDTO contactDTO) {
        log.info("Adding new contact for user: {}", contactDTO.getUserId());

        // Ensure ID and create time are generated
        if (contactDTO.getId() == null) {
            contactDTO.setId(System.currentTimeMillis()); // Custom ID generation (can use snowflake in prod)
        }
        contactDTO.setCreateTime(LocalDateTime.now());

        Contact contact = new Contact();
        contact.setId(contactDTO.getId());
        contact.setUserId(contactDTO.getUserId());
        contact.setContactName(contactDTO.getContactName());
        contact.setContactPinyin(contactDTO.getContactPinyin());
        contact.setContactInitial(contactDTO.getContactInitial());
        contact.setBankName(contactDTO.getBankName());
        contact.setAccountNo(contactDTO.getAccountNo());
        contact.setPhone(contactDTO.getPhone());
        contact.setCreateTime(contactDTO.getCreateTime());

        // 1. Save to MySQL
        Contact savedContact = contactRepository.save(contact);

        // 2. Prepare ES Document
        ContactDocument document = contactMapper.toDocument(savedContact);

        // 3. Send event to Kafka for ES sync
        contactProducer.sendContactSyncEvent(document);

        return contactMapper.toDTO(savedContact);
    }

    @Override
    public List<ContactDTO> searchContacts(Long userId, String keyword) {
        log.info("Searching contacts for user: {} with keyword: {}", userId, keyword);

        // Retrieve recent contacts to boost their score
        Set<String> recentContacts = getRecentContacts(userId);

        // Search in Elasticsearch
        List<ContactDocument> searchResults = contactSearchRepository.searchContacts(userId, keyword, recentContacts);

        return searchResults.stream()
                .map(contactMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getRecentContacts(Long userId) {
        String key = RECENT_CONTACTS_KEY_PREFIX + userId;
        // Retrieve top 10 recent contacts by score (timestamp), highest score first
        return stringRedisTemplate.opsForZSet().reverseRange(key, 0, 9);
    }

    @Override
    public void addRecentContact(Long userId, String contactName) {
        String key = RECENT_CONTACTS_KEY_PREFIX + userId;
        long score = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().add(key, contactName, score);
        log.info("Added recent contact {} for user {}", contactName, userId);
    }
}
