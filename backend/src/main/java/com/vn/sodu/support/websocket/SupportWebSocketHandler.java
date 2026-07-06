package com.vn.sodu.support.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.dto.SupportPrincipalDTO;
import com.vn.sodu.support.dto.WebSocketEvent;
import com.vn.sodu.support.dto.WebSocketMessage;
import com.vn.sodu.support.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupportWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final SupportService supportService;
    private final ObjectMapper objectMapper;

    private final Map<Long, WebSocketSession> customerSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> staffSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        WebSocketMessage wsMessage;
        try {
            wsMessage = objectMapper.readValue(textMessage.getPayload(), WebSocketMessage.class);
        } catch (Exception e) {
            sendError(session, "Invalid message format");
            return;
        }

        if (wsMessage.getType() == null) {
            sendError(session, "Message type is required");
            return;
        }

        switch (wsMessage.getType().toUpperCase()) {
            case "AUTH" -> handleAuth(session, wsMessage);
            case "SEND_MESSAGE" -> handleSendMessage(session, wsMessage);
            default -> sendError(session, "Unknown message type: " + wsMessage.getType());
        }
    }

    private void handleAuth(WebSocketSession session, WebSocketMessage message) throws IOException {
        String token = message.getAccessToken();
        if (token == null || token.isBlank()) {
            sendErrorAndClose(session, "Access token is required");
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            sendErrorAndClose(session, "Invalid or expired access token");
            return;
        }

        String email = jwtService.extractUsername(token);
        SupportPrincipalDTO principal;
        try {
            principal = supportService.getSupportPrincipal(email);
        } catch (NotFoundException e) {
            sendErrorAndClose(session, "Account not found");
            return;
        }
        if (!principal.active()) {
            sendErrorAndClose(session, "Account is not active");
            return;
        }

        session.getAttributes().put("supportPrincipal", principal);

        if (principal.staff()) {
            staffSessions.put(session.getId(), session);
            log.info("Staff authenticated: {} (session {})", principal.email(), session.getId());
        } else {
            customerSessions.put(principal.accountId(), session);
            log.info("Customer authenticated: {} (session {})", principal.email(), session.getId());
        }

        sendEvent(session, WebSocketEvent.builder()
                .type("AUTH_SUCCESS")
                .build());
    }

    private void handleSendMessage(WebSocketSession session, WebSocketMessage message) throws IOException {
        Map<String, Object> attrs = session.getAttributes();
        SupportPrincipalDTO sender = (attrs != null) ? (SupportPrincipalDTO) attrs.get("supportPrincipal") : null;
        if (sender == null) {
            sendError(session, "Authentication required. Send AUTH frame first.");
            return;
        }

        if (message.getContent() == null || message.getContent().isBlank()) {
            sendError(session, "Message content is required");
            return;
        }

        String role = sender.staff() ? sender.roleName() : "USER";

        try {
            Long conversationId;
            if (sender.staff()) {
                if (message.getConversationId() == null) {
                    sendError(session, "conversationId is required for staff messages");
                    return;
                }
                conversationId = message.getConversationId();
            } else {
                conversationId = supportService.getOrCreateConversationSummary(sender.email()).getId();
            }

            MessageResponseDTO dto = supportService.sendMessage(
                    sender.accountId(), role, conversationId, message.getContent());
            WebSocketEvent event = WebSocketEvent.builder()
                    .type("MESSAGE_CREATED")
                    .message(dto)
                    .build();

            broadcastToConversation(conversationId, event);
        } catch (Exception e) {
            log.error("Failed to send message", e);
            sendError(session, "Failed to send message: " + e.getMessage());
        }
    }

    private void broadcastToConversation(Long conversationId, WebSocketEvent event) throws IOException {
        String payload = objectMapper.writeValueAsString(event);

        Long customerId = supportService.getConversationCustomerId(conversationId);

        WebSocketSession customerSession = customerSessions.get(customerId);
        if (customerSession != null && customerSession.isOpen()) {
            customerSession.sendMessage(new TextMessage(payload));
        }

        for (WebSocketSession staffSession : staffSessions.values()) {
            if (staffSession.isOpen()) {
                staffSession.sendMessage(new TextMessage(payload));
            }
        }
    }

    private void sendEvent(WebSocketSession session, WebSocketEvent event) throws IOException {
        if (session.isOpen()) {
            String payload = objectMapper.writeValueAsString(event);
            session.sendMessage(new TextMessage(payload));
        }
    }

    private void sendError(WebSocketSession session, String error) throws IOException {
        WebSocketEvent event = WebSocketEvent.builder()
                .type("ERROR")
                .error(error)
                .build();
        sendEvent(session, event);
    }

    private void sendErrorAndClose(WebSocketSession session, String error) throws IOException {
        sendError(session, error);
        session.close(CloseStatus.POLICY_VIOLATION);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SupportPrincipalDTO principal = (SupportPrincipalDTO) session.getAttributes().get("supportPrincipal");
        if (principal != null) {
            if (principal.staff()) {
                staffSessions.remove(session.getId());
                log.info("Staff disconnected: {} (session {})", principal.email(), session.getId());
            } else {
                customerSessions.remove(principal.accountId());
                log.info("Customer disconnected: {} (session {})", principal.email(), session.getId());
            }
        }
        session.getAttributes().clear();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error on session {}: {}", session.getId(), exception.getMessage());
    }

}
