package com.authserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 놀이기구 엔티티
 * 놀이기구의 정보와 수용 인원을 관리합니다.
 */
@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ride_id")
    private Long rideId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "riding_time", nullable = false)
    private Integer ridingTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "capacity_total", nullable = false)
    private Integer capacityTotal;

    @Column(name = "capacity_premium", nullable = false)
    private Integer capacityPremium;

    @Column(name = "capacity_general", nullable = false)
    private Integer capacityGeneral;

    @Column(name = "short_description", length = 128)
    private String shortDescription;

    @Column(name = "long_description", length = 1024)
    private String longDescription;

    @Column(name = "photo", length = 300)
    private String photo;

    @Column(name = "operating_time", length = 20)
    private String operatingTime;
}
