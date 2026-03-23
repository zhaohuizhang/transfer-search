package com.bank.transfersearch.dto;

import lombok.Data;

@Data
public class SearchRequest {
    private Long userId;
    private String keyword;
}
