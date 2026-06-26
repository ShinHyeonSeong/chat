package com.shin.chat.controller;

import com.shin.chat.domain.dto.chat.ChatRoomDetailDto;
import com.shin.chat.domain.dto.chat.ChatRoomSummaryDto;
import com.shin.chat.domain.dto.chat.CreateDirectRoomRequestDto;
import com.shin.chat.domain.dto.chat.CreateGroupRoomRequestDto;
import com.shin.chat.jwt.CustomUserDetails;
import com.shin.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/direct")
    public ResponseEntity<ChatRoomDetailDto> createDirectRoom(
            // spring security context에 저장된 인증객체 (현재 로그인한 유저)를 가져온다.
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateDirectRoomRequestDto request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(chatRoomService.createDirectRoom(userDetails.getUser(), request.getTargetUserId()));
    }

    @PostMapping("/group")
    public ResponseEntity<ChatRoomDetailDto> createGroupRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateGroupRoomRequestDto request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(chatRoomService.createGroupRoom(userDetails.getUser(), request));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ChatRoomDetailDto> joinRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
                return ResponseEntity.ok(chatRoomService.joinRoom(userDetails.getUser(), id));
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomSummaryDto>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
                return ResponseEntity.ok(chatRoomService.getMyRooms(userDetails.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomDetailDto> getRoomDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
                return ResponseEntity.ok(chatRoomService.getRoomDetail(userDetails.getUser(), id));
    }
}
