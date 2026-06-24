package com.shin.chat.controller;

import com.shin.chat.domain.dto.chat.MessagePageDto;
import com.shin.chat.domain.dto.chat.ReadReceiptRequestDto;
import com.shin.chat.jwt.CustomUserDetails;
import com.shin.chat.service.MessageService;
import com.shin.chat.service.ReadReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ReadReceiptService readReceiptService;

    @GetMapping("/{id}/messages")
    public ResponseEntity<MessagePageDto> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(
                messageService.getMessages(userDetails.getUser(), id, cursorId, size));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> updateReadReceipt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody ReadReceiptRequestDto request) {
        readReceiptService.updateReadReceipt(userDetails.getUser(), id, request.getLastReadMessageId());
        return ResponseEntity.noContent().build();
    }
}
