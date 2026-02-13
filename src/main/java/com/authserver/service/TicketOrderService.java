package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.dto.TicketOrderResponseDto;
import com.authserver.entity.TicketOrder;
import com.authserver.entity.ActiveStatus;
import com.authserver.entity.Ticket;
import com.authserver.entity.TicketManagement;
import com.authserver.entity.TicketType;
import com.authserver.repository.TicketOrderRepository;
import com.authserver.repository.TicketRepository;
import com.authserver.repository.TicketManagementRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 티켓 주문 내역 관리 Service
 */
@Service
@RequiredArgsConstructor
public class TicketOrderService {

    private final TicketOrderRepository ticketOrderRepository;
    private final TicketRepository ticketRepository;
    private final TicketManagementRepository ticketManagementRepository;

    /**
     * 티켓 주문 생성 (티켓 관리 ID 사용)
     */
    @Transactional
    public TicketOrder createTicketOrder(Long userId, Long ticketManagementId) {
        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setUserId(userId);
        ticketOrder.setTicketManagementId(ticketManagementId);
        ticketOrder.setPaymentDate(LocalDateTime.now());
        ticketOrder.setActiveStatus(ActiveStatus.DEACTIVE);

        return ticketOrderRepository.save(ticketOrder);
    }

    /**
     * 티켓 주문 생성 (이용 날짜와 티켓 타입 사용)
     */
    @Transactional
    public TicketOrder createTicketOrderByDateAndType(Long userId, LocalDateTime availableAt, TicketType ticketType) {
        // 1. 티켓 타입으로 티켓 찾기
        List<Ticket> tickets = ticketRepository.findByTicketType(ticketType);
        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Ticket not found for type: " + ticketType);
        }

        // 2. 첫 번째 티켓의 ID 사용
        Long ticketId = tickets.get(0).getTicketId();

        // 3. 티켓 ID와 이용 날짜로 티켓 관리 찾기
        List<TicketManagement> ticketManagements =
            ticketManagementRepository.findByTicketIdAndAvailableAt(ticketId, availableAt);

        if (ticketManagements.isEmpty()) {
            throw new IllegalArgumentException("TicketManagement not found for ticketId: " + ticketId + " and availableAt: " + availableAt);
        }

        // 4. 첫 번째 티켓 관리 사용
        Long ticketManagementId = ticketManagements.get(0).getTicketManagementId();

        // 5. 티켓 주문 생성
        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setUserId(userId);
        ticketOrder.setTicketManagementId(ticketManagementId);
        ticketOrder.setPaymentDate(LocalDateTime.now());
        ticketOrder.setActiveStatus(ActiveStatus.DEACTIVE);

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
     * 사용자의 오늘 이후 주문 조회
     */
    @Transactional(readOnly = true)
    public List<TicketOrder> getUserTicketOrders(Long userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ticketOrderRepository.findByUserIdAndAvailableAtAfterToday(userId, today);
    }

    /**
     * 사용자의 활성 상태이면서 오늘 이후 주문 조회
     */
    @Transactional(readOnly = true)
    public List<TicketOrder> getUserActiveTicketOrders(Long userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ticketOrderRepository.findByUserIdAndActiveStatusAndAvailableAtAfterToday(userId, ActiveStatus.ACTIVE, today);
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

    /**
     * TicketOrder를 TicketOrderResponseDto로 변환
     */
    private TicketOrderResponseDto convertToResponseDto(TicketOrder ticketOrder) {
        // TicketManagement 조회
        TicketManagement ticketManagement = ticketManagementRepository.findById(ticketOrder.getTicketManagementId())
            .orElseThrow(() -> new IllegalArgumentException("TicketManagement not found"));

        // Ticket 조회
        Ticket ticket = ticketRepository.findById(ticketManagement.getTicketId())
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        return TicketOrderResponseDto.builder()
            .ticketOrderId(ticketOrder.getTicketOrderId())
            .userId(ticketOrder.getUserId())
            .availableAt(ticketManagement.getAvailableAt())
            .ticketType(ticket.getTicketType())
            .paymentDate(ticketOrder.getPaymentDate())
            .activeStatus(ticketOrder.getActiveStatus())
            .build();
    }

    /**
     * 사용자의 오늘 이후 주문 조회 (DTO 반환)
     */
    @Transactional(readOnly = true)
    public List<TicketOrderResponseDto> getUserTicketOrdersDto(Long userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<TicketOrder> ticketOrders = ticketOrderRepository.findByUserIdAndAvailableAtAfterToday(userId, today);
        return ticketOrders.stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * 사용자의 활성 상태이면서 오늘 이후 주문 조회 (DTO 반환)
     */
    @Transactional(readOnly = true)
    public List<TicketOrderResponseDto> getUserActiveTicketOrdersDto(Long userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<TicketOrder> ticketOrders = ticketOrderRepository.findByUserIdAndActiveStatusAndAvailableAtAfterToday(userId, ActiveStatus.ACTIVE, today);
        return ticketOrders.stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }
}
