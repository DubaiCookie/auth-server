package com.authserver.dto.queue;

import java.util.List;

/**
 * 전체 놀이기구 대기열 정보 응답
 */
public record RideQueueInfoListResponse(
        List<RideQueueInfoDto> rides
) {
}

