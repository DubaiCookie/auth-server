package com.authserver.dto.queue;

import com.authserver.entity.Ride;

import java.util.List;

/**
 * 놀이기구 정보와 대기열 정보를 합친 응답 DTO
 */
public record RideWithQueueInfoDto(
        Long rideId,
        String name,
        Integer ridingTime,
        Boolean isActive,
        Integer capacityTotal,
        Integer capacityPremium,
        Integer capacityGeneral,
        String shortDescription,
        String longDescription,
        String photo,
        String operatingTime,
        List<RideWaitTimeDto> waitTimes
) {
    public static RideWithQueueInfoDto from(Ride ride, List<RideWaitTimeDto> waitTimes) {
        return new RideWithQueueInfoDto(
                ride.getRideId(),
                ride.getName(),
                ride.getRidingTime(),
                ride.getIsActive(),
                ride.getCapacityTotal(),
                ride.getCapacityPremium(),
                ride.getCapacityGeneral(),
                ride.getShortDescription(),
                ride.getLongDescription(),
                ride.getPhoto(),
                ride.getOperatingTime(),
                waitTimes
        );
    }
}

