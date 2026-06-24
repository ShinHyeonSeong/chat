package com.shin.chat.controller;

import com.shin.chat.domain.dto.chat.MessagePageDto;
import com.shin.chat.domain.dto.chat.MessageResponseDto;
import com.shin.chat.domain.entity.UserEntity;
import com.shin.chat.domain.entity.UserRoleEntity;
import com.shin.chat.exception.NotRoomMemberException;
import com.shin.chat.jwt.CustomUserDetails;
import com.shin.chat.jwt.CustomUserDetailsService;
import com.shin.chat.jwt.JwtManager;
import com.shin.chat.jwt.config.SecurityConfig;
import com.shin.chat.service.MessageService;
import com.shin.chat.service.ReadReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
@Import(SecurityConfig.class)
class MessageControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean MessageService messageService;
    @MockitoBean ReadReceiptService readReceiptService;
    @MockitoBean JwtManager jwtManager;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private UserEntity mockUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        UserRoleEntity role = mock(UserRoleEntity.class);
        when(role.getName()).thenReturn("USER");

        mockUser = mock(UserEntity.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getUsername()).thenReturn("user_a");
        when(mockUser.getRole()).thenReturn(role);

        userDetails = new CustomUserDetails(mockUser);
    }

    private MessageResponseDto sampleMessage(Long id) {
        return new MessageResponseDto(id, 1L, "user_a", "메시지 " + id, LocalDateTime.now(), false);
    }

    // ── PATCH /api/rooms/{id}/read ────────────────────────────────────────────

    @Test
    void updateReadReceipt_returns204() throws Exception {
        willDoNothing().given(readReceiptService)
                .updateReadReceipt(eq(mockUser), eq(1L), eq(10L));

        mockMvc.perform(patch("/api/rooms/1/read")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReadMessageId\": 10}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateReadReceipt_notMember_returns403() throws Exception {
        willThrow(new NotRoomMemberException())
                .given(readReceiptService).updateReadReceipt(any(), anyLong(), anyLong());

        mockMvc.perform(patch("/api/rooms/1/read")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReadMessageId\": 10}"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/rooms/{id}/messages ──────────────────────────────────────────

    @Test
    void getMessages_firstPage_returnsNextCursor() throws Exception {
        List<MessageResponseDto> messages = List.of(sampleMessage(5L), sampleMessage(4L));
        // nextCursor가 non-null → 다음 페이지 존재
        given(messageService.getMessages(any(), anyLong(), isNull(), anyInt()))
                .willReturn(new MessagePageDto(messages, 4L));

        mockMvc.perform(get("/api/rooms/1/messages?size=2")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").value(4));
    }

    @Test
    void getMessages_lastPage_returnsNullNextCursor() throws Exception {
        List<MessageResponseDto> messages = List.of(sampleMessage(3L), sampleMessage(2L));
        // nextCursor가 null → 마지막 페이지
        given(messageService.getMessages(any(), anyLong(), eq(5L), anyInt()))
                .willReturn(new MessagePageDto(messages, null));

        mockMvc.perform(get("/api/rooms/1/messages?cursorId=5&size=10")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }
}
