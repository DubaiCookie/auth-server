package com.authserver.dto.queue;

import jakarta.validation.constraints.NotNull;

/**
 * 대기열 등록 요청 DTO
 *
 * 클라이언트는 userId, rideId, ticketType만 전송하며,
 * 서버에서 자동으로 활성화된 티켓을 찾아 처리합니다.
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

