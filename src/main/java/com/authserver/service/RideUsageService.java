package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.entity.RideUsage;
import com.authserver.entity.RideUsageStatus;
import com.authserver.repository.RideUsageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RideUsageService {

    private static final Logger logger = LoggerFactory.getLogger(RideUsageService.class);
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
     * 대기열 예약 가능 여부 확인
     * - 이미 COMPLETED 상태인 경우 예약 불가
     * - 이미 WAITING 상태인 경우 중복 예약 불가
     *
     * @param ticketOrderId 티켓 주문 ID
     * @param rideId 놀이기구 ID
     * @return 예약 가능하면 true, 불가능하면 false
     */
    @Transactional(readOnly = true)
    public boolean canEnqueue(Long ticketOrderId, Long rideId) {
        // 1. 이미 완료된 이용 기록이 있는지 확인
        Optional<RideUsage> completedUsage = rideUsageRepository.findByTicketOrderIdAndRideIdAndStatus(
                ticketOrderId, rideId, RideUsageStatus.COMPLETED);

        if (completedUsage.isPresent()) {
            logger.warn("이미 이용 완료된 놀이기구 - ticketOrderId={}, rideId={}", ticketOrderId, rideId);
            return false;
        }

        // 2. 이미 대기 중인 예약이 있는지 확인
        Optional<RideUsage> waitingUsage = rideUsageRepository.findByTicketOrderIdAndRideIdAndStatus(
                ticketOrderId, rideId, RideUsageStatus.WAITED);

        if (waitingUsage.isPresent()) {
            logger.warn("이미 대기 중인 예약 존재 - ticketOrderId={}, rideId={}", ticketOrderId, rideId);
            return false;
        }

        return true;
    }

    /**
     * 특정 티켓으로 특정 놀이기구 이용 기록 조회
     */
    @Transactional(readOnly = true)
    public List<RideUsage> getRideUsagesByTicketAndRide(Long ticketOrderId, Long rideId) {
        return rideUsageRepository.findByTicketOrderIdAndRideId(ticketOrderId, rideId);
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
     * 이용 완료 처리 (기존 메서드)
     */
    @Transactional
    public RideUsage completeRideUsage(Long rideUsageId) {
        RideUsage rideUsage = getRideUsage(rideUsageId);
        rideUsage.setStatus(RideUsageStatus.COMPLETED);
        rideUsage.setCompletedAt(LocalDateTime.now());
        return rideUsageRepository.save(rideUsage);
    }

    /**
     * 놀이기구 이용 완료 처리 (userId와 rideId로 처리)
     * WAITED 상태의 레코드를 찾아서 COMPLETED로 변경
     *
     * @param userId 사용자 ID
     * @param rideId 놀이기구 ID
     * @return 완료 처리된 RideUsage
     * @throws IllegalArgumentException WAITED 상태의 레코드가 없는 경우
     */
    @Transactional
    public RideUsage completeRideByUserAndRide(Long userId, Long rideId) {
        logger.info("놀이기구 이용 완료 처리 시작 - userId={}, rideId={}", userId, rideId);

        // WAITED 상태인 레코드 찾기
        Optional<RideUsage> waitedUsage = rideUsageRepository.findByUserIdAndRideIdAndStatus(
                userId, rideId, RideUsageStatus.WAITED);

        if (waitedUsage.isEmpty()) {
            logger.warn("대기 중인 놀이기구 예약이 없음 - userId={}, rideId={}", userId, rideId);
            throw new IllegalArgumentException("해당 놀이기구에 대기 중인 예약이 없습니다.");
        }

        RideUsage rideUsage = waitedUsage.get();
        rideUsage.setStatus(RideUsageStatus.COMPLETED);
        rideUsage.setCompletedAt(LocalDateTime.now());

        RideUsage saved = rideUsageRepository.save(rideUsage);
        logger.info("놀이기구 이용 완료 처리 성공 - rideUsageId={}, userId={}, rideId={}",
                saved.getRideUsageId(), userId, rideId);

        return saved;
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
