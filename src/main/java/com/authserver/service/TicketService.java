package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.entity.Ticket;
import com.authserver.entity.TicketType;
import com.authserver.repository.TicketRepository;

import java.util.List;

/**
 * 티켓 상품 정보 관리 Service
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    /**
     * 티켓 상품 생성
     */
    @Transactional
    public Ticket createTicket(TicketType ticketType, Integer ticketCount, Integer price) {
        Ticket ticket = new Ticket();
        ticket.setTicketType(ticketType);
        ticket.setTicketCount(ticketCount);
        ticket.setPrice(price);

        return ticketRepository.save(ticket);
    }

    /**
     * 티켓 상품 조회
     */
    @Transactional(readOnly = true)
    public Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    /**
     * 모든 티켓 상품 조회
     */
    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    /**
     * 티켓 타입으로 조회
     */
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByType(TicketType ticketType) {
        return ticketRepository.findByTicketType(ticketType);
    }

    /**
     * 티켓 상품 수정
     */
    @Transactional
    public Ticket updateTicket(Long ticketId, TicketType ticketType, Integer ticketCount, Integer price) {
        Ticket ticket = getTicket(ticketId);
        if (ticketType != null) {
            ticket.setTicketType(ticketType);
        }
        if (ticketCount != null) {
            ticket.setTicketCount(ticketCount);
        }
        if (price != null) {
            ticket.setPrice(price);
        }
        return ticketRepository.save(ticket);
    }

    /**
     * 티켓 상품 삭제
     */
    @Transactional
    public void deleteTicket(Long ticketId) {
        ticketRepository.deleteById(ticketId);
    }
}
