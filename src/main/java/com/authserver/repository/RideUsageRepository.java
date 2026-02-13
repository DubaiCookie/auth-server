package com.authserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.authserver.entity.RideUsage;
import com.authserver.entity.RideUsageStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideUsageRepository extends JpaRepository<RideUsage, Long> {

    List<RideUsage> findByUserId(Long userId);

    List<RideUsage> findByRideId(Long rideId);

    List<RideUsage> findByTicketOrderId(Long ticketOrderId);

    List<RideUsage> findByStatus(RideUsageStatus status);

    List<RideUsage> findByUserIdAndStatus(Long userId, RideUsageStatus status);

    /**
     * 특정 티켓으로 특정 놀이기구 이용 내역 조회
     */
    List<RideUsage> findByTicketOrderIdAndRideId(Long ticketOrderId, Long rideId);

    /**
     * 특정 티켓으로 특정 놀이기구를 특정 상태로 이용한 내역 조회
     */
    Optional<RideUsage> findByTicketOrderIdAndRideIdAndStatus(Long ticketOrderId, Long rideId, RideUsageStatus status);

    /**
     * 사용자가 특정 놀이기구에 특정 상태인지 확인
     */
    Optional<RideUsage> findByUserIdAndRideIdAndStatus(Long userId, Long rideId, RideUsageStatus status);
}
