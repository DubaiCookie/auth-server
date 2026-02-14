package com.authserver.dto.queue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 대기열 서버로 취소 요청을 보낼 때 사용하는 내부 DTO
 * (userId, rideId, ticketType 포함)
 */
public record CancelQueueServerRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId,

        @NotNull(message = "놀이기구 ID는 필수입니다")
        Long rideId,

        @NotBlank(message = "티켓 타입은 필수입니다")
        String ticketType
) {
}

