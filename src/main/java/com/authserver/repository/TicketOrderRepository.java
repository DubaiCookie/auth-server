package com.authserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.authserver.entity.TicketOrder;
import com.authserver.entity.ActiveStatus;

import java.util.List;

/**
 * 티켓 주문 내역 Repository
 */
@Repository
public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {

    /**
     * 사용자 ID로 모든 주문 조회
     */
    List<TicketOrder> findByUserId(Long userId);

    /**
     * 사용자 ID와 활성 상태로 주문 조회
     */
    List<TicketOrder> findByUserIdAndActiveStatus(Long userId, ActiveStatus activeStatus);

    /**
     * 티켓 관리 ID로 주문 조회
     */
    List<TicketOrder> findByTicketManagementId(Long ticketManagementId);
}
