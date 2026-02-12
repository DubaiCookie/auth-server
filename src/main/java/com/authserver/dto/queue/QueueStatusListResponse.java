package com.authserver.dto.queue;

import java.util.List;

/**
 * 대기열 상태 리스트 응답 DTO
 */
public record QueueStatusListResponse(
        List<QueueStatusItem> items
) {
}

