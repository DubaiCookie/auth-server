package com.authserver.dto.queue;

/**
 * 놀이기구 이용 완료 응답 DTO
 */
public record CompleteRideResponse(
        boolean success,
        String message,
        Long userId,
        Long rideId
) {
}

