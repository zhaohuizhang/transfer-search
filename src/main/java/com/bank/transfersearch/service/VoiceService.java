package com.bank.transfersearch.service;

import com.bank.transfersearch.dto.ContactDTO;
import java.util.List;

public interface VoiceService {
    List<ContactDTO> voiceSearch(Long userId, String text);
}
