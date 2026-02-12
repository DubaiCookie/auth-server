package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.entity.TicketOrder;
import com.authserver.entity.ActiveStatus;
import com.authserver.repository.TicketOrderRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 티켓 주문 내역 관리 Service
 */
@Service
@RequiredArgsConstructor
public class TicketOrderService {

    private final TicketOrderRepository ticketOrderRepository;

    /**
     * 티켓 주문 생성
     */
    @Transactional
    public TicketOrder createTicketOrder(Long userId, Long ticketManagementId) {
        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setUserId(userId);
        ticketOrder.setTicketManagementId(ticketManagementId);
        ticketOrder.setPaymentDate(LocalDateTime.now());
        ticketOrder.setActiveStatus(ActiveStatus.ACTIVE);

        return ticketOrderRepository.save(ticketOrder);
    }

    /**
     * 티켓 주문 조회
     */
    @Transactional(readOnly = true)
    public TicketOrder getTicketOrder(Long ticketOrderId) {
        return ticketOrderRepository.findById(ticketOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket order not found"));
    }

    /**
     * 사용자의 모든 주문 조회
     */
    @Transactional(readOnly = true)
    public List<TicketOrder> getUserTicketOrders(Long userId) {
        return ticketOrderRepository.findByUserId(userId);
    }

    /**
     * 사용자의 활성 주문 조회
     */
    @Transactional(readOnly = true)
    public List<TicketOrder> getUserActiveTicketOrders(Long userId) {
        return ticketOrderRepository.findByUserIdAndActiveStatus(userId, ActiveStatus.ACTIVE);
    }

    /**
     * 티켓 주문 상태 변경
     */
    @Transactional
    public TicketOrder updateTicketOrderStatus(Long ticketOrderId, ActiveStatus status) {
        TicketOrder ticketOrder = getTicketOrder(ticketOrderId);
        ticketOrder.setActiveStatus(status);
        return ticketOrderRepository.save(ticketOrder);
    }

    /**
     * 티켓 주문 삭제
     */
    @Transactional
    public void deleteTicketOrder(Long ticketOrderId) {
        ticketOrderRepository.deleteById(ticketOrderId);
    }
}
