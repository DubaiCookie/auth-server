package com.authserver.dto.queue;

import jakarta.validation.constraints.NotNull;

/**
 * 대기열 취소 요청 DTO
 */
public record CancelRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId,

        @NotNull(message = "놀이기구 ID는 필수입니다")
        Long rideId
) {
}

