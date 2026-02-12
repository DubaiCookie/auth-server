package com.authserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.authserver.entity.RideUsage;
import com.authserver.entity.RideUsageStatus;

import java.util.List;

@Repository
public interface RideUsageRepository extends JpaRepository<RideUsage, Long> {

    List<RideUsage> findByUserId(Long userId);

    List<RideUsage> findByRideId(Long rideId);

    List<RideUsage> findByTicketOrderId(Long ticketOrderId);

    List<RideUsage> findByStatus(RideUsageStatus status);

    List<RideUsage> findByUserIdAndStatus(Long userId, RideUsageStatus status);
}
