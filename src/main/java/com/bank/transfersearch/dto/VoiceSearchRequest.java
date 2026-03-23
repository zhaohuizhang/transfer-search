package com.bank.transfersearch.dto;

import lombok.Data;

@Data
public class VoiceSearchRequest {
    private String text;
    private Long userId;
}
