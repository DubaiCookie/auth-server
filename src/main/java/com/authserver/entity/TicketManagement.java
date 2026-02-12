package com.authserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 티켓 재고 관리 엔티티
 * 특정 날짜/시간에 판매 가능한 티켓의 재고를 관리합니다.
 */
@Entity
@Table(name = "ticket_management")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_management_id")
    private Long ticketManagementId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "stock", nullable = false)
    private Integer stock;
}
