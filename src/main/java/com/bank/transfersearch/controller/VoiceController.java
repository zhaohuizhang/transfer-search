package com.bank.transfersearch.controller;

import com.bank.transfersearch.dto.ContactDTO;
import com.bank.transfersearch.dto.VoiceSearchRequest;
import com.bank.transfersearch.service.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
@Tag(name = "Voice Search API", description = "Endpoints for parsing voice commands")
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping("/search")
    @Operation(summary = "Voice search for transfer contacts")
    public ResponseEntity<List<ContactDTO>> voiceSearch(@RequestBody VoiceSearchRequest request) {
        List<ContactDTO> results = voiceService.voiceSearch(request.getUserId(), request.getText());
        return ResponseEntity.ok(results);
    }
}
