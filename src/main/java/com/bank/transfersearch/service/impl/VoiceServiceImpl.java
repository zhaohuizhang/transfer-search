package com.bank.transfersearch.service.impl;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.service.ContactService;
import com.bank.transfersearch.service.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceServiceImpl implements VoiceService {

    private final ContactService contactService;

    // Pattern to extract name like "给张三转账" -> "张三"
    private static final Pattern PATTERN_GEI_ZHUANZHANG = Pattern.compile("给(.*?)转账");

    @Override
    public List<ContactDTO> voiceSearch(Long userId, String text) {
        log.info("Voice search text: {} for user: {}", text, userId);

        String keyword = parseVoiceText(text);

        if (keyword != null && !keyword.trim().isEmpty()) {
            log.info("Parsed keyword: {}", keyword);
            return contactService.searchContacts(userId, keyword);
        } else {
            // Fallback to searching the whole text
            return contactService.searchContacts(userId, text);
        }
    }

    private String parseVoiceText(String text) {
        if (text == null) {
            return "";
        }

        Matcher matcher = PATTERN_GEI_ZHUANZHANG.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Basic fallback filtering
        String filteredText = text.replace("给", "")
                .replace("转账", "")
                .replace("打钱", "")
                .trim();

        return filteredText;
    }
}
