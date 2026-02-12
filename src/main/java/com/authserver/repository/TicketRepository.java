package com.authserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.authserver.entity.Ticket;
import com.authserver.entity.TicketType;

import java.util.List;
import java.util.Optional;

/**
 * 티켓 상품 정보 Repository
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * 티켓 타입으로 조회
     */
    List<Ticket> findByTicketType(TicketType ticketType);

    /**
     * 티켓 타입과 개수로 조회
     */
    Optional<Ticket> findByTicketTypeAndTicketCount(TicketType ticketType, Integer ticketCount);
}
