package com.authserver.dto.websocket;

import java.util.List;

/**
 * 전체 놀이기구 대기 시간 이벤트
 * WebSocket 목적지: /sub/rides/minutes
 */
public record AllRidesMinutesEvent(
        List<RideMinutes> rides
) {
    public record RideMinutes(
            int rideId,
            int estimatedWaitMinutes
    ) {
    }
}

