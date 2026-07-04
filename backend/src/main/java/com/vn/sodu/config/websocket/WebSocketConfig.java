package com.vn.sodu.config.websocket;

import com.vn.sodu.config.CorsProperties;
import com.vn.sodu.support.websocket.SupportWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SupportWebSocketHandler supportWebSocketHandler;
    private final CorsProperties corsProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(supportWebSocketHandler, "/ws/support")
                .setAllowedOrigins(corsProperties.getAllowedOrigins());
    }
}
