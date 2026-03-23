package com.bank.transfersearch.service.impl;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VoiceServiceImplTest {

    @Mock
    private ContactService contactService;

    @InjectMocks
    private VoiceServiceImpl voiceService;

    private List<ContactDTO> mockContacts;

    @BeforeEach
    void setUp() {
        ContactDTO contact1 = new ContactDTO();
        contact1.setContactName("张三");
        ContactDTO contact2 = new ContactDTO();
        contact2.setContactName("李四");
        mockContacts = Arrays.asList(contact1, contact2);
    }

    @Test
    void voiceSearch_WithExactMatchPattern_ShouldParseAndSearch() {
        // Arrange
        Long userId = 1L;
        String text = "给张三转账";
        when(contactService.searchContacts(eq(userId), eq("张三"))).thenReturn(mockContacts);

        // Act
        List<ContactDTO> results = voiceService.voiceSearch(userId, text);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void voiceSearch_WithFallbackPattern_ShouldFilterAndSearch() {
        // Arrange
        Long userId = 1L;
        String text = "我要给王五打钱";
        when(contactService.searchContacts(eq(userId), eq("我要王五"))).thenReturn(mockContacts);

        // Act
        List<ContactDTO> results = voiceService.voiceSearch(userId, text);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void voiceSearch_WithNullText_ShouldUseFallbackSearch() {
        // Arrange
        Long userId = 1L;
        String text = null;
        when(contactService.searchContacts(eq(userId), eq(null))).thenReturn(mockContacts);

        // Act
        List<ContactDTO> results = voiceService.voiceSearch(userId, text);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
    }
}
