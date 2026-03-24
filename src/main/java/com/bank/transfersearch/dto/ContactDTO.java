package com.bank.transfersearch.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data
public class ContactDTO {
    private Long id;
    private Long userId;
    private String contactName;
    private String contactPinyin;
    private String contactInitial;
    private String bankName;
    private String accountNo;
    private String phone;
    private LocalDateTime createTime;
    private String highlightName;
    private Double score;
    private List<String> matchedFields;
}
