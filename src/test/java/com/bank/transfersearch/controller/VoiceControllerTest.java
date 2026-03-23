package com.bank.transfersearch.controller;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.dto.VoiceSearchRequest;
import com.bank.transfersearch.service.VoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoiceController.class)
public class VoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoiceService voiceService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<ContactDTO> mockContacts;

    @BeforeEach
    void setUp() {
        ContactDTO contact1 = new ContactDTO();
        contact1.setContactName("张三");
        contact1.setAccountNo("123456789");
        mockContacts = Arrays.asList(contact1);
    }

    @Test
    void searchByVoice_ShouldReturnContacts() throws Exception {
        VoiceSearchRequest request = new VoiceSearchRequest();
        request.setUserId(1L);
        request.setText("给张三转账");

        when(voiceService.voiceSearch(eq(1L), eq("给张三转账"))).thenReturn(mockContacts);

        mockMvc.perform(post("/voice/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactName").value("张三"));
    }
}
