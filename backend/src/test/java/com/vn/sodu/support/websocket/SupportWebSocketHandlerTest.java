package com.vn.sodu.support.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.support.SupportConversation;
import com.vn.sodu.support.SupportMessage;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.dto.WebSocketMessage;
import com.vn.sodu.support.service.SupportService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.Account.AccountStatus;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportWebSocketHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private SupportService supportService;

    @Mock
    private WebSocketSession session;

    private SupportWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new SupportWebSocketHandler(jwtService, accountRepo, supportService, objectMapper);
    }

    @Test
    void invalidToken_closesSession() throws Exception {
        WebSocketMessage msg = WebSocketMessage.builder()
                .type("AUTH")
                .accessToken("invalid-token")
                .build();

        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(msg)));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void missingAuth_beforeSend_rejectsMessage() throws Exception {
        WebSocketMessage msg = WebSocketMessage.builder()
                .type("SEND_MESSAGE")
                .content("Hello")
                .build();

        when(session.getAttributes()).thenReturn(new java.util.HashMap<>());
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(msg)));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        String sent = captor.getValue().getPayload();
        assertThat(sent).contains("Authentication required");
    }

    @Test
    void validAuth_doesNotCloseSession() throws Exception {
        Account account = Account.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("User")
                .role(Role.builder().name("USER").build())
                .status(AccountStatus.ACTIVE)
                .build();

        WebSocketMessage msg = WebSocketMessage.builder()
                .type("AUTH")
                .accessToken("valid-token")
                .build();

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(accountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(supportService.isStaff(account)).thenReturn(false);
        when(session.getAttributes()).thenReturn(new java.util.HashMap<>());
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(msg)));

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void sendMessage_persistsMessage() throws Exception {
        Account customer = Account.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("User")
                .role(Role.builder().name("USER").build())
                .status(AccountStatus.ACTIVE)
                .build();

        SupportConversation conversation = SupportConversation.builder()
                .id(10L)
                .account(customer)
                .build();

        SupportMessage savedMessage = SupportMessage.builder()
                .id(100L)
                .conversation(conversation)
                .sender(customer)
                .senderRole("USER")
                .content("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(session.getAttributes()).thenReturn(new java.util.HashMap<>() {{
            put("account", customer);
        }});
        when(supportService.isStaff(customer)).thenReturn(false);
        when(supportService.getOrCreateConversation(customer)).thenReturn(conversation);
        when(supportService.sendMessage(customer, "USER", 10L, "Hello")).thenReturn(savedMessage);

        MessageResponseDTO dto = MessageResponseDTO.builder()
                .id(100L)
                .conversationId(10L)
                .senderId(1L)
                .senderEmail("user@example.com")
                .senderRole("USER")
                .content("Hello")
                .build();
        when(supportService.toMessageResponse(savedMessage)).thenReturn(dto);
        when(supportService.getConversationById(10L)).thenReturn(conversation);

        WebSocketMessage msg = WebSocketMessage.builder()
                .type("SEND_MESSAGE")
                .content("Hello")
                .build();

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(msg)));

        verify(supportService).sendMessage(customer, "USER", 10L, "Hello");
    }
}
