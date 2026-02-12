package com.authserver.dto.queue;

import java.util.List;

public record RideQueueInfoDto(
        int rideId,
        List<RideWaitTimeDto> waitTimes
) {
}

