package com.bank.transfersearch.mapper;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.entity.Contact;
import com.bank.transfersearch.entity.ContactDocument;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ContactMapper {

    public ContactDTO toDTO(Contact contact) {
        if (contact == null) {
            return null;
        }
        ContactDTO dto = new ContactDTO();
        dto.setId(contact.getId());
        dto.setUserId(contact.getUserId());
        dto.setContactName(contact.getContactName());
        dto.setContactPinyin(contact.getContactPinyin());
        dto.setContactInitial(contact.getContactInitial());
        dto.setBankName(contact.getBankName());
        dto.setAccountNo(contact.getAccountNo());
        dto.setPhone(contact.getPhone());
        dto.setCreateTime(contact.getCreateTime());
        return dto;
    }

    public ContactDTO toDTO(ContactDocument document) {
        if (document == null) {
            return null;
        }
        ContactDTO dto = new ContactDTO();
        dto.setId(document.getId());
        dto.setUserId(document.getUserId());
        dto.setContactName(document.getContactName());
        dto.setContactPinyin(document.getContactPinyin());
        dto.setContactInitial(document.getContactInitial());
        dto.setBankName(document.getBankName());
        dto.setPhone(document.getPhone());
        dto.setCreateTime(document.getCreateTime());
        dto.setHighlightName(document.getHighlightName());
        return dto;
    }

    public ContactDocument toDocument(Contact contact) {
        if (contact == null) {
            return null;
        }
        ContactDocument document = new ContactDocument();
        document.setId(contact.getId());
        document.setUserId(contact.getUserId());
        document.setContactName(contact.getContactName());
        document.setContactPinyin(contact.getContactPinyin());
        document.setContactInitial(contact.getContactInitial());
        document.setBankName(contact.getBankName());
        document.setPhone(contact.getPhone());
        document.setCreateTime(contact.getCreateTime());
        
        List<String> suggest = new ArrayList<>();
        if (contact.getContactPinyin() != null) suggest.add(contact.getContactPinyin());
        if (contact.getContactName() != null) suggest.add(contact.getContactName());
        if (contact.getContactInitial() != null) suggest.add(contact.getContactInitial());
        document.setContactSuggest(suggest);
        
        return document;
    }
}
