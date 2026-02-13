package com.authserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.authserver.entity.TicketManagement;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 티켓 재고 관리 Repository
 */
@Repository
public interface TicketManagementRepository extends JpaRepository<TicketManagement, Long> {

    /**
     * 티켓 ID로 재고 관리 조회
     */
    List<TicketManagement> findByTicketId(Long ticketId);

    /**
     * 특정 날짜 이후 판매 가능한 티켓 조회
     */
    List<TicketManagement> findByAvailableAtAfter(LocalDateTime dateTime);

    /**
     * 재고가 있는 티켓 조회
     */
    List<TicketManagement> findByStockGreaterThan(Integer stock);

    /**
     * 특정 날짜 이후의 모든 재고 조회
     */
    List<TicketManagement> findByAvailableAtGreaterThanEqual(LocalDateTime dateTime);

    /**
     * 티켓 ID와 이용 날짜로 재고 조회
     */
    List<TicketManagement> findByTicketIdAndAvailableAt(Long ticketId, LocalDateTime availableAt);
}
