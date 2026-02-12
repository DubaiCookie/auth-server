package com.authserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 티켓 주문 엔티티
 * 사용자가 구매한 티켓 내역을 관리합니다.
 */
@Entity
@Table(name = "ticket_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_order_id")
    private Long ticketOrderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ticket_management_id", nullable = false)
    private Long ticketManagementId;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_status", nullable = false)
    private ActiveStatus activeStatus;
}
