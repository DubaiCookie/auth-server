package com.authserver.dto.queue;

/**
 * 대기열 취소 응답 DTO
 */
public record CancelResponse(
        boolean success,
        String message,
        Long userId,
        Long rideId
) {
}

