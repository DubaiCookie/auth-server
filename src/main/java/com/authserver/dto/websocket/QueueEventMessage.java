package com.authserver.dto.websocket;

/**
 * 웹소켓으로 전송할 대기열 이벤트 DTO
 */
public record QueueEventMessage(
        Long rideId,
        String rideName,    // 놀이기구 이름
        Long userId,
        String type,        // PREMIUM or GENERAL
        String status       // READY or ALMOST_READY
) {
}

