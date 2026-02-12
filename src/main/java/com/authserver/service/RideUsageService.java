package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.entity.RideUsage;
import com.authserver.entity.RideUsageStatus;
import com.authserver.repository.RideUsageRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RideUsageService {

    private final RideUsageRepository rideUsageRepository;

    /**
     * 놀이기구 이용 기록 생성 (대기 시작)
     */
    @Transactional
    public RideUsage createRideUsage(Long userId, Long rideId, Long ticketOrderId) {
        RideUsage rideUsage = new RideUsage();
        rideUsage.setUserId(userId);
        rideUsage.setRideId(rideId);
        rideUsage.setTicketOrderId(ticketOrderId);
        rideUsage.setStatus(RideUsageStatus.WAITED);
        rideUsage.setCreatedAt(LocalDateTime.now());

        return rideUsageRepository.save(rideUsage);
    }

    /**
     * 이용 기록 조회
     */
    @Transactional(readOnly = true)
    public RideUsage getRideUsage(Long rideUsageId) {
        return rideUsageRepository.findById(rideUsageId)
                .orElseThrow(() -> new IllegalArgumentException("RideUsage not found"));
    }

    /**
     * 사용자의 이용 기록 조회
     */
    @Transactional(readOnly = true)
    public List<RideUsage> getUserRideUsages(Long userId) {
        return rideUsageRepository.findByUserId(userId);
    }

    /**
     * 놀이기구별 이용 기록 조회
     */
    @Transactional(readOnly = true)
    public List<RideUsage> getRideUsagesByRide(Long rideId) {
        return rideUsageRepository.findByRideId(rideId);
    }

    /**
     * 상태별 이용 기록 조회
     */
    @Transactional(readOnly = true)
    public List<RideUsage> getRideUsagesByStatus(RideUsageStatus status) {
        return rideUsageRepository.findByStatus(status);
    }

    /**
     * 입장 가능 알림 시각 기록
     */
    @Transactional
    public RideUsage setArrivedAt(Long rideUsageId) {
        RideUsage rideUsage = getRideUsage(rideUsageId);
        rideUsage.setArrivedAt(LocalDateTime.now());
        return rideUsageRepository.save(rideUsage);
    }

    /**
     * 이용 완료 처리
     */
    @Transactional
    public RideUsage completeRideUsage(Long rideUsageId) {
        RideUsage rideUsage = getRideUsage(rideUsageId);
        rideUsage.setStatus(RideUsageStatus.COMPLETED);
        rideUsage.setCompletedAt(LocalDateTime.now());
        return rideUsageRepository.save(rideUsage);
    }

    /**
     * 노쇼 처리
     */
    @Transactional
    public RideUsage markAsNoShow(Long rideUsageId) {
        RideUsage rideUsage = getRideUsage(rideUsageId);
        rideUsage.setStatus(RideUsageStatus.NO_SHOW);
        return rideUsageRepository.save(rideUsage);
    }

    /**
     * 이용 기록 삭제
     */
    @Transactional
    public void deleteRideUsage(Long rideUsageId) {
        rideUsageRepository.deleteById(rideUsageId);
    }
}
