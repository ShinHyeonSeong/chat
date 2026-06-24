package com.shin.chat.controller;

import tools.jackson.databind.ObjectMapper;
import com.shin.chat.domain.dto.chat.ChatRoomDetailDto;
import com.shin.chat.domain.dto.chat.ChatRoomSummaryDto;
import com.shin.chat.domain.dto.chat.CreateDirectRoomRequestDto;
import com.shin.chat.domain.dto.chat.CreateGroupRoomRequestDto;
import com.shin.chat.domain.entity.UserEntity;
import com.shin.chat.domain.entity.UserRoleEntity;
import com.shin.chat.exception.NotRoomMemberException;
import com.shin.chat.exception.RoomNotFoundException;
import com.shin.chat.jwt.CustomUserDetails;
import com.shin.chat.jwt.CustomUserDetailsService;
import com.shin.chat.jwt.JwtManager;
import com.shin.chat.service.ChatRoomService;
import com.shin.chat.jwt.config.SecurityConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
@Import(SecurityConfig.class)
class ChatRoomControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ChatRoomService chatRoomService;
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

    private ChatRoomDetailDto sampleDetailDto(Long roomId, String type) {
        return new ChatRoomDetailDto(roomId, type, type.equals("GROUP") ? "팀 채팅" : null,
                List.of(new ChatRoomDetailDto.MemberInfo(1L, "user_a", LocalDateTime.now()),
                        new ChatRoomDetailDto.MemberInfo(2L, "user_b", LocalDateTime.now())),
                null, null);
    }

    // ── POST /api/rooms/direct ────────────────────────────────────────────────

    @Test
    void createDirectRoom_returns201WithBody() throws Exception {
        ChatRoomDetailDto response = sampleDetailDto(1L, "ONE");
        given(chatRoomService.createDirectRoom(any(UserEntity.class), eq(2L))).willReturn(response);

        mockMvc.perform(post("/api/rooms/direct")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("ONE"))
                .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void createDirectRoom_withoutToken_returns403() throws Exception {
        mockMvc.perform(post("/api/rooms/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\": 2}"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/rooms/group ─────────────────────────────────────────────────

    @Test
    void createGroupRoom_returns201WithBody() throws Exception {
        ChatRoomDetailDto response = sampleDetailDto(2L, "GROUP");
        given(chatRoomService.createGroupRoom(any(UserEntity.class), any(CreateGroupRoomRequestDto.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/rooms/group")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"팀 채팅\", \"invitedUserIds\": [2]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.name").value("팀 채팅"));
    }

    // ── POST /api/rooms/{id}/join ─────────────────────────────────────────────

    @Test
    void joinRoom_returns200WithBody() throws Exception {
        ChatRoomDetailDto response = sampleDetailDto(3L, "GROUP");
        given(chatRoomService.joinRoom(any(UserEntity.class), eq(3L))).willReturn(response);

        mockMvc.perform(post("/api/rooms/3/join")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L));
    }

    @Test
    void joinRoom_notFound_returns404() throws Exception {
        given(chatRoomService.joinRoom(any(UserEntity.class), eq(99L)))
                .willThrow(new RoomNotFoundException());

        mockMvc.perform(post("/api/rooms/99/join")
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/rooms ────────────────────────────────────────────────────────

    @Test
    void getMyRooms_returns200WithList() throws Exception {
        List<ChatRoomSummaryDto> response = List.of(
                new ChatRoomSummaryDto(1L, "ONE", "user_b", "안녕", LocalDateTime.now(), 2),
                new ChatRoomSummaryDto(2L, "GROUP", "팀 채팅", null, null, 0)
        );
        given(chatRoomService.getMyRooms(any(UserEntity.class))).willReturn(response);

        mockMvc.perform(get("/api/rooms").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].unreadCount").value(2))
                .andExpect(jsonPath("$[1].name").value("팀 채팅"));
    }

    // ── GET /api/rooms/{id} ───────────────────────────────────────────────────

    @Test
    void getRoomDetail_returns200WithBody() throws Exception {
        ChatRoomDetailDto response = sampleDetailDto(1L, "ONE");
        given(chatRoomService.getRoomDetail(any(UserEntity.class), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/rooms/1").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void getRoomDetail_notMember_returns403() throws Exception {
        given(chatRoomService.getRoomDetail(any(UserEntity.class), eq(1L)))
                .willThrow(new NotRoomMemberException());

        mockMvc.perform(get("/api/rooms/1").with(user(userDetails)))
                .andExpect(status().isForbidden());
    }
}
