package com.authserver.dto.queue;

/**
 * 대기열 등록 응답 DTO
 */
public record EnqueueResponse(
        Long position,
        Integer estimatedWaitMinutes
) {
}

