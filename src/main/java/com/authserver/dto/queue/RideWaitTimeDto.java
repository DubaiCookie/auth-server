package com.authserver.dto.queue;

public record RideWaitTimeDto(
        String ticketType,
        int waitingCount,
        int estimatedWaitMinutes
) {
}

