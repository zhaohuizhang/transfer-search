package com.bank.transfersearch.service.impl;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.entity.Contact;
import com.bank.transfersearch.entity.ContactDocument;
import com.bank.transfersearch.kafka.ContactProducer;
import com.bank.transfersearch.mapper.ContactMapper;
import com.bank.transfersearch.repository.ContactRepository;
import com.bank.transfersearch.search.ContactSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private ContactSearchRepository contactSearchRepository;

    @Mock
    private ContactMapper contactMapper;

    @Mock
    private ContactProducer contactProducer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private ContactServiceImpl contactService;

    private ContactDTO sampleContactDTO;
    private Contact sampleContact;
    private ContactDocument sampleDocument;

    @BeforeEach
    void setUp() {
        sampleContactDTO = new ContactDTO();
        sampleContactDTO.setUserId(1L);
        sampleContactDTO.setContactName("Test User");
        sampleContactDTO.setContactPinyin("TestUser");
        sampleContactDTO.setContactInitial("T");
        sampleContactDTO.setBankName("Test Bank");
        sampleContactDTO.setAccountNo("123456789");
        sampleContactDTO.setPhone("13800000000");

        sampleContact = new Contact();
        sampleContact.setId(1001L);
        sampleContact.setUserId(1L);
        sampleContact.setContactName("Test User");

        sampleDocument = new ContactDocument();
        sampleDocument.setId(1001L);
        sampleDocument.setUserId(1L);
        sampleDocument.setContactName("Test User");
    }

    @Test
    void addContact_SuccessfullyAddsContact() {
        // Arrange
        when(contactRepository.save(any(Contact.class))).thenReturn(sampleContact);
        when(contactMapper.toDocument(any(Contact.class))).thenReturn(sampleDocument);
        when(contactMapper.toDTO(any(Contact.class))).thenReturn(sampleContactDTO);

        // Act
        ContactDTO result = contactService.addContact(sampleContactDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test User", result.getContactName());
        verify(contactRepository, times(1)).save(any(Contact.class));
        verify(contactProducer, times(1)).sendContactSyncEvent(any(ContactDocument.class));
    }

    @Test
    void searchContacts_ReturnsContactsSuccessfully() {
        // Arrange
        Long userId = 1L;
        String keyword = "Test";
        Set<String> recentContacts = new HashSet<>(Arrays.asList("Test User"));
        List<ContactDocument> searchResults = Arrays.asList(sampleDocument);

        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(anyString(), eq(0L), eq(9L))).thenReturn(recentContacts);
        when(contactSearchRepository.searchContacts(userId, keyword, recentContacts)).thenReturn(searchResults);
        when(contactMapper.toDTO(any(ContactDocument.class))).thenReturn(sampleContactDTO);

        // Act
        List<ContactDTO> results = contactService.searchContacts(userId, keyword);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test User", results.get(0).getContactName());
        verify(contactSearchRepository, times(1)).searchContacts(userId, keyword, recentContacts);
    }

    @Test
    void getRecentContacts_ReturnsRecentContacts() {
        // Arrange
        Long userId = 1L;
        Set<String> expectedContacts = new HashSet<>(Arrays.asList("Contact1", "Contact2"));
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange("recent:contacts:" + userId, 0, 9)).thenReturn(expectedContacts);

        // Act
        Set<String> result = contactService.getRecentContacts(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Contact1"));
        verify(zSetOperations, times(1)).reverseRange("recent:contacts:" + userId, 0, 9);
    }

    @Test
    void addRecentContact_SuccessfullyAddsRecentContact() {
        // Arrange
        Long userId = 1L;
        String contactName = "New Contact";
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // Act
        contactService.addRecentContact(userId, contactName);

        // Assert
        verify(zSetOperations, times(1)).add(eq("recent:contacts:" + userId), eq(contactName), anyDouble());
    }
}
