package com.authserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.entity.Ride;
import com.authserver.repository.RideRepository;
import com.authserver.dto.queue.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RideService {

    private static final Logger logger = LoggerFactory.getLogger(RideService.class);
    private final RideRepository rideRepository;
    private final QueueClientService queueClientService;

    public RideService(RideRepository rideRepository, @Lazy QueueClientService queueClientService) {
        this.rideRepository = rideRepository;
        this.queueClientService = queueClientService;
    }

    /**
     * 놀이기구 생성
     */
    @Transactional
    public Ride createRide(String name, Integer ridingTime, Boolean isActive,
                          Integer capacityTotal, Integer capacityPremium, Integer capacityGeneral,
                          String shortDescription, String longDescription, String photo, String operatingTime) {
        Ride ride = new Ride();
        ride.setName(name);
        ride.setRidingTime(ridingTime);
        ride.setIsActive(isActive);
        ride.setCapacityTotal(capacityTotal);
        ride.setCapacityPremium(capacityPremium);
        ride.setCapacityGeneral(capacityGeneral);
        ride.setShortDescription(shortDescription);
        ride.setLongDescription(longDescription);
        ride.setPhoto(photo);
        ride.setOperatingTime(operatingTime);
        ride.setCreatedAt(LocalDateTime.now());

        return rideRepository.save(ride);
    }

    /**
     * 놀이기구 조회
     */
    @Transactional(readOnly = true)
    public Ride getRide(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
    }

    /**
     * 특정 놀이기구 조회 (대기열 정보 포함)
     */
    @Transactional(readOnly = true)
    public RideDetailDto getRideWithQueueInfo(Long rideId) {
        logger.info("놀이기구 상세 조회 (대기열 정보 포함) - rideId={}", rideId);

        // 1. 놀이기구 정보 조회
        Ride ride = getRide(rideId);

        // 2. 대기열 서버에서 해당 놀이기구의 대기열 정보 조회
        try {
            RideQueueInfoDto queueInfo = queueClientService.getRideQueueInfo(rideId);
            logger.info("대기열 정보 조회 성공 - rideId={}, waitTimes={}", rideId, queueInfo.waitTimes().size());

            return RideDetailDto.from(ride, queueInfo.waitTimes());
        } catch (Exception e) {
            logger.error("대기열 정보 조회 실패 - rideId={}", rideId, e);
            // 대기열 정보 조회 실패 시 빈 리스트 반환
            return RideDetailDto.from(ride, List.of());
        }
    }

    /**
     * 모든 놀이기구 조회
     */
    @Transactional(readOnly = true)
    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    /**
     * 운영 중인 놀이기구 조회
     */
    @Transactional(readOnly = true)
    public List<Ride> getActiveRides() {
        return rideRepository.findByIsActive(true);
    }

    /**
     * 운영 중인 놀이기구 조회 (대기열 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<RideWithQueueInfoDto> getActiveRidesWithQueueInfo() {
        // 1. 운영 중인 놀이기구 조회
        List<Ride> rides = rideRepository.findByIsActive(true);

        // 2. 대기열 서버에서 모든 놀이기구의 대기열 정보 조회
        RideQueueInfoListResponse queueInfo = queueClientService.getAllRidesQueueInfo();

        // 3. 놀이기구 ID를 키로 하는 대기열 정보 맵 생성
        Map<Integer, List<RideWaitTimeDto>> queueInfoMap = queueInfo.rides().stream()
                .collect(Collectors.toMap(
                        RideQueueInfoDto::rideId,
                        RideQueueInfoDto::waitTimes
                ));

        // 4. 놀이기구 정보와 대기열 정보 결합
        return rides.stream()
                .map(ride -> {
                    List<RideWaitTimeDto> waitTimes = queueInfoMap.getOrDefault(
                            ride.getRideId().intValue(),
                            List.of()  // 대기열 정보가 없으면 빈 리스트
                    );
                    return RideWithQueueInfoDto.from(ride, waitTimes);
                })
                .collect(Collectors.toList());
    }

    /**
     * 이름으로 놀이기구 검색
     */
    @Transactional(readOnly = true)
    public List<Ride> searchRidesByName(String name) {
        return rideRepository.findByNameContaining(name);
    }

    /**
     * 놀이기구 정보 수정
     */
    @Transactional
    public Ride updateRide(Long rideId, String name, Integer ridingTime, Boolean isActive,
                          Integer capacityTotal, Integer capacityPremium, Integer capacityGeneral,
                          String shortDescription, String longDescription, String photo, String operatingTime) {
        Ride ride = getRide(rideId);
        if (name != null) ride.setName(name);
        if (ridingTime != null) ride.setRidingTime(ridingTime);
        if (isActive != null) ride.setIsActive(isActive);
        if (capacityTotal != null) ride.setCapacityTotal(capacityTotal);
        if (capacityPremium != null) ride.setCapacityPremium(capacityPremium);
        if (capacityGeneral != null) ride.setCapacityGeneral(capacityGeneral);
        if (shortDescription != null) ride.setShortDescription(shortDescription);
        if (longDescription != null) ride.setLongDescription(longDescription);
        if (photo != null) ride.setPhoto(photo);
        if (operatingTime != null) ride.setOperatingTime(operatingTime);

        return rideRepository.save(ride);
    }

    /**
     * 놀이기구 삭제
     */
    @Transactional
    public void deleteRide(Long rideId) {
        rideRepository.deleteById(rideId);
    }
}
