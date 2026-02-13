package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.entity.TicketManagement;
import com.authserver.repository.TicketManagementRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 티켓 재고 관리 Service
 */
@Service
@RequiredArgsConstructor
public class TicketManagementService {

    private final TicketManagementRepository ticketManagementRepository;

    /**
     * 티켓 재고 생성
     */
    @Transactional
    public TicketManagement createTicketManagement(Long ticketId, LocalDateTime availableAt, Integer stock) {
        TicketManagement ticketManagement = new TicketManagement();
        ticketManagement.setTicketId(ticketId);
        ticketManagement.setAvailableAt(availableAt);
        ticketManagement.setStock(stock);

        return ticketManagementRepository.save(ticketManagement);
    }

    /**
     * 티켓 재고 조회
     */
    @Transactional(readOnly = true)
    public TicketManagement getTicketManagement(Long ticketManagementId) {
        return ticketManagementRepository.findById(ticketManagementId)
                .orElseThrow(() -> new IllegalArgumentException("TicketManagement not found"));
    }

    /**
     * 모든 티켓 재고 조회
     */
    @Transactional(readOnly = true)
    public List<TicketManagement> getAllTicketManagements() {
        return ticketManagementRepository.findAll();
    }

    /**
     * 특정 티켓의 재고 조회
     */
    @Transactional(readOnly = true)
    public List<TicketManagement> getTicketManagementsByTicketId(Long ticketId) {
        return ticketManagementRepository.findByTicketId(ticketId);
    }

    /**
     * 특정 날짜 이후 판매 가능한 티켓 조회
     */
    @Transactional(readOnly = true)
    public List<TicketManagement> getAvailableTicketManagements(LocalDateTime dateTime) {
        return ticketManagementRepository.findByAvailableAtAfter(dateTime);
    }

    /**
     * 재고가 있는 티켓 조회
     */
    @Transactional(readOnly = true)
    public List<TicketManagement> getInStockTicketManagements(Integer minStock) {
        return ticketManagementRepository.findByStockGreaterThan(minStock);
    }

    /**
     * 티켓 재고 정보 수정
     */
    @Transactional
    public TicketManagement updateTicketManagement(Long ticketManagementId, Long ticketId, 
                                                    LocalDateTime availableAt, Integer stock) {
        TicketManagement ticketManagement = getTicketManagement(ticketManagementId);
        if (ticketId != null) ticketManagement.setTicketId(ticketId);
        if (availableAt != null) ticketManagement.setAvailableAt(availableAt);
        if (stock != null) ticketManagement.setStock(stock);

        return ticketManagementRepository.save(ticketManagement);
    }

    /**
     * 티켓 재고 삭제
     */
    @Transactional
    public void deleteTicketManagement(Long ticketManagementId) {
        ticketManagementRepository.deleteById(ticketManagementId);
    }

    /**
     * 오늘 포함 이후 모든 티켓 재고 조회
     */
    @Transactional(readOnly = true)
    public List<TicketManagement> getAllTicketManagementsFromToday() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ticketManagementRepository.findByAvailableAtGreaterThanEqual(today);
    }
}
