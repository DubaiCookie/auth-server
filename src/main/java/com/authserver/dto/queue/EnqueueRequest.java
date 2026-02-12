package com.authserver.dto.queue;

import jakarta.validation.constraints.NotNull;

/**
 * 대기열 등록 요청 DTO
 */
public record EnqueueRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotNull(message = "놀이기구 ID는 필수입니다.")
        Long rideId,

        @NotNull(message = "티켓 타입은 필수입니다.")
        String ticketType
) {
}

