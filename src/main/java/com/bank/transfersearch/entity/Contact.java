package com.bank.transfersearch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transfer_contact")
public class Contact {

    @Id
    private Long id;

    private Long userId;

    private String contactName;

    private String contactPinyin;

    private String contactInitial;

    private String bankName;

    private String accountNo;

    private String phone;

    private LocalDateTime createTime;

}
