package com.authserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 놀이기구 이용 내역 엔티티
 * 사용자가 놀이기구를 이용한 내역을 관리합니다.
 */
@Entity
@Table(name = "ride_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ride_usage_id")
    private Long rideUsageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "ticket_order_id", nullable = false)
    private Long ticketOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RideUsageStatus status;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
