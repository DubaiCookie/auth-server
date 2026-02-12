package com.authserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 연결을 관리하는 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Tag(name = "WebSocket API", description = "대기열 이벤트를 위한 WebSocket 연결")
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    /**
     * 클라이언트가 특정 사용자의 대기열 이벤트를 구독할 때 호출
     * 경로: /queue/subscribe/{userId}
     */
    @MessageMapping("/queue/subscribe/{userId}")
    @Operation(summary = "대기열 이벤트 구독", description = "사용자가 대기열 이벤트를 받기 위해 웹소켓을 구독합니다.")
    public void subscribeQueue(@DestinationVariable("userId") Long userId, Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";

        logger.info("WebSocket 대기열 구독 - 사용자ID={}, Principal={}", userId, username);
    }
}

