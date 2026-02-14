package com.authserver.dto.websocket;

import java.util.List;

/**
 * 사용자별 대기열 상태 이벤트
 * WebSocket 목적지: /sub/user/{userId}/queue-status
 *
 * Kafka READY/ALMOST_READY 이벤트도 같은 채널로 전송됨
 */
public record UserQueueStatusEvent(
        Long userId,
        List<QueueItem> items
) {
    public record QueueItem(
            Long rideId,
            String rideName,
            String ticketType,       // PREMIUM or GENERAL
            int position,            // 현재 순번
            int estimatedWaitMinutes // 예상 대기 시간 (분)
    ) {
    }
}

