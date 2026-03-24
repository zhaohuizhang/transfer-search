package com.bank.transfersearch.controller;

import com.bank.transfersearch.dto.AnalyzeResponseDTO;
import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.dto.SearchResponseDTO;
import com.bank.transfersearch.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@Tag(name = "Contact API", description = "Endpoints for managing and searching contacts")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Add a new transfer contact")
    public ResponseEntity<ContactDTO> addContact(@RequestBody ContactDTO contactDTO) {
        ContactDTO savedContact = contactService.addContact(contactDTO);
        return ResponseEntity.ok(savedContact);
    }

    @GetMapping("/search")
    @Operation(summary = "Search contacts using Elasticsearch")
    public ResponseEntity<SearchResponseDTO> searchContacts(
            @RequestParam Long userId,
            @RequestParam String keyword) {

        SearchResponseDTO response = contactService.searchContacts(userId, keyword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyze")
    @Operation(summary = "Simulate Elasticsearch analyzer")
    public ResponseEntity<AnalyzeResponseDTO> analyze(
            @RequestParam String text,
            @RequestParam(defaultValue = "pinyin_analyzer") String analyzer) {
        List<String> tokens = contactService.analyze(text, analyzer);
        return ResponseEntity.ok(new AnalyzeResponseDTO(tokens));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Suggest contacts using Elasticsearch completion suggester")
    public ResponseEntity<List<String>> suggestContacts(
            @RequestParam(required = false) Long userId,
            @RequestParam String prefix) {
        List<String> results = contactService.suggestContacts(userId, prefix);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent contacts for a user")
    public ResponseEntity<Set<String>> getRecentContacts(@RequestParam Long userId) {
        Set<String> recentContacts = contactService.getRecentContacts(userId);
        return ResponseEntity.ok(recentContacts);
    }

    @PostMapping("/recent")
    @Operation(summary = "Add to recent contacts manually (for testing)")
    public ResponseEntity<Void> addRecentContact(
            @RequestParam Long userId,
            @RequestParam String contactName) {
        contactService.addRecentContact(userId, contactName);
        return ResponseEntity.ok().build();
    }
}
