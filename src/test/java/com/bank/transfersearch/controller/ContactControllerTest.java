package com.bank.transfersearch.controller;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContactController.class)
public class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactService contactService;

    @Autowired
    private ObjectMapper objectMapper;

    private ContactDTO sampleContactDTO;

    @BeforeEach
    void setUp() {
        sampleContactDTO = new ContactDTO();
        sampleContactDTO.setId(1001L);
        sampleContactDTO.setUserId(1L);
        sampleContactDTO.setContactName("Test User");
        sampleContactDTO.setContactPinyin("TestUser");
        sampleContactDTO.setContactInitial("T");
        sampleContactDTO.setBankName("Test Bank");
        sampleContactDTO.setAccountNo("123456789");
        sampleContactDTO.setPhone("13800000000");
        sampleContactDTO.setCreateTime(LocalDateTime.now());
    }

    @Test
    void addContact_ShouldReturnSavedContact() throws Exception {
        when(contactService.addContact(any(ContactDTO.class))).thenReturn(sampleContactDTO);

        mockMvc.perform(post("/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleContactDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("Test User"))
                .andExpect(jsonPath("$.accountNo").value("123456789"));
    }

    @Test
    void searchContacts_ShouldReturnListOfContacts() throws Exception {
        List<ContactDTO> contacts = Arrays.asList(sampleContactDTO);
        when(contactService.searchContacts(eq(1L), eq("Test"))).thenReturn(contacts);

        mockMvc.perform(get("/contacts/search")
                .param("userId", "1")
                .param("keyword", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactName").value("Test User"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRecentContacts_ShouldReturnSetOfRecentContacts() throws Exception {
        Set<String> recentContacts = new HashSet<>(Arrays.asList("Test User 1", "Test User 2"));
        when(contactService.getRecentContacts(eq(1L))).thenReturn(recentContacts);

        mockMvc.perform(get("/contacts/recent")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void addRecentContact_ShouldReturnOk() throws Exception {
        doNothing().when(contactService).addRecentContact(eq(1L), eq("Test User"));

        mockMvc.perform(post("/contacts/recent")
                .param("userId", "1")
                .param("contactName", "Test User"))
                .andExpect(status().isOk());
    }
}
