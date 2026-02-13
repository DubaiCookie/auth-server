package com.authserver.dto.queue;

import com.authserver.entity.Ride;

import java.util.List;

/**
 * 특정 놀이기구 상세 정보 (대기열 정보 포함)
 */
public record RideDetailDto(
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
    public static RideDetailDto from(Ride ride, List<RideWaitTimeDto> waitTimes) {
        return new RideDetailDto(
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

