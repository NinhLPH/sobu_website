package com.vn.sodu.support.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.support.ConversationStatus;
import com.vn.sodu.support.dto.ConversationSummaryDTO;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.dto.SupportPrincipalDTO;
import com.vn.sodu.support.dto.WebSocketMessage;
import com.vn.sodu.support.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportWebSocketHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private SupportService supportService;

    @Mock
    private WebSocketSession session;

    private SupportWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new SupportWebSocketHandler(jwtService, supportService, objectMapper);
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
        SupportPrincipalDTO principal = SupportPrincipalDTO.builder()
                .accountId(1L)
                .email("user@example.com")
                .roleName("USER")
                .staff(false)
                .active(true)
                .build();

        WebSocketMessage msg = WebSocketMessage.builder()
                .type("AUTH")
                .accessToken("valid-token")
                .build();

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(supportService.getSupportPrincipal("user@example.com")).thenReturn(principal);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(msg)));

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void authThenSendMessage_persistsMessageAndBroadcasts() throws Exception {
        SupportPrincipalDTO principal = SupportPrincipalDTO.builder()
                .accountId(1L)
                .email("user@example.com")
                .roleName("USER")
                .staff(false)
                .active(true)
                .build();

        ConversationSummaryDTO conversation = ConversationSummaryDTO.builder()
                .id(10L)
                .status(ConversationStatus.OPEN)
                .build();

        MessageResponseDTO dto = MessageResponseDTO.builder()
                .id(100L)
                .conversationId(10L)
                .senderId(1L)
                .senderEmail("user@example.com")
                .senderRole("USER")
                .content("Hello")
                .build();

        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        when(session.isOpen()).thenReturn(true);
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(supportService.getSupportPrincipal("user@example.com")).thenReturn(principal);
        when(supportService.getOrCreateConversationSummary("user@example.com")).thenReturn(conversation);
        when(supportService.sendMessage(1L, "USER", 10L, "Hello")).thenReturn(dto);
        when(supportService.getConversationCustomerId(10L)).thenReturn(1L);

        WebSocketMessage auth = WebSocketMessage.builder()
                .type("AUTH")
                .accessToken("valid-token")
                .build();

        WebSocketMessage msg = WebSocketMessage.builder()
                .type("SEND_MESSAGE")
                .content("Hello")
                .build();

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(auth)));
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(msg)));

        verify(supportService).sendMessage(1L, "USER", 10L, "Hello");
        verify(supportService).getConversationCustomerId(10L);
    }
}
