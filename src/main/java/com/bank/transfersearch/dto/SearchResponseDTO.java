package com.bank.transfersearch.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchResponseDTO {
    private List<ContactDTO> results;
    private String dsl;
    private Long took;
}
