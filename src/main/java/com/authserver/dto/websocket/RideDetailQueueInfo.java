package com.authserver.dto.websocket;

import java.util.List;

/**
 * 특정 놀이기구의 상세 대기열 정보
 * WebSocket 목적지: /sub/rides/{rideId}/info
 */
public record RideDetailQueueInfo(
        Long rideId,
        List<WaitTime> waitTimes
) {
    public record WaitTime(
            String ticketType,      // PREMIUM or GENERAL
            int waitingCount,       // 대기 인원
            int estimatedWaitMinutes // 예상 대기 시간 (분)
    ) {
    }
}

