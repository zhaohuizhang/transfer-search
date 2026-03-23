package com.bank.transfersearch.dto;

import lombok.Data;

import java.time.LocalDateTime;

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
}
