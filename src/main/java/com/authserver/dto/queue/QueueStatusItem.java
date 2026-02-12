package com.authserver.dto.queue;

/**
 * 개별 대기열 상태 DTO
 */
public record QueueStatusItem(
        Long rideId,
        String rideName,
        String ticketType,
        Long position,
        Integer estimatedWaitMinutes
) {
}

