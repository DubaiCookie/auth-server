package com.authserver.dto.queue;

import java.util.List;

public record RideQueueInfoListResponse(
        List<RideQueueInfoDto> rides
) {
}

