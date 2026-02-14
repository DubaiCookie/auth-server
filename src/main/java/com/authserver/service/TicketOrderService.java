package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.dto.TicketOrderResponseDto;
import com.authserver.entity.TicketOrder;
import com.authserver.entity.ActiveStatus;
import com.authserver.entity.TicketManagement;
import com.authserver.entity.Ticket;
import com.authserver.entity.TicketType;
import com.authserver.repository.TicketOrderRepository;
import com.authserver.repository.TicketManagementRepository;
import com.authserver.repository.TicketRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 티켓 주문 내역 관리 Service
 */
@Service
@RequiredArgsConstructor
public class TicketOrderService {

    private final TicketOrderRepository ticketOrderRepository;
    private final TicketManagementRepository ticketManagementRepository;
    private final TicketRepository ticketRepository;

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

    /**
     * 사용자의 오늘 날짜 활성 티켓 조회
     * ACTIVE 상태이면서 available_at이 오늘 날짜인 티켓을 반환합니다.
     *
     * @param userId 사용자 ID
     * @return 오늘 날짜 활성 티켓 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<TicketOrder> getTodayActiveTicket(Long userId) {
        // 오늘 날짜의 시작과 끝 시간 계산
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // 사용자의 활성 티켓 주문 조회
        List<TicketOrder> activeTickets = ticketOrderRepository.findByUserIdAndActiveStatus(userId, ActiveStatus.ACTIVE);

        // available_at이 오늘 날짜인 티켓 필터링
        return activeTickets.stream()
                .filter(ticketOrder -> {
                    TicketManagement ticketManagement = ticketManagementRepository
                            .findById(ticketOrder.getTicketManagementId())
                            .orElse(null);

                    if (ticketManagement == null) {
                        return false;
                    }

                    LocalDateTime availableAt = ticketManagement.getAvailableAt();
                    return !availableAt.isBefore(startOfDay) && !availableAt.isAfter(endOfDay);
                })
                .findFirst();
    }

    /**
     * 티켓 주문의 티켓 타입 조회
     *
     * @param ticketOrder 티켓 주문
     * @return 티켓 타입
     * @throws IllegalArgumentException 티켓을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public TicketType getTicketType(TicketOrder ticketOrder) {
        // TicketManagement 조회
        TicketManagement ticketManagement = ticketManagementRepository
                .findById(ticketOrder.getTicketManagementId())
                .orElseThrow(() -> new IllegalArgumentException("티켓 관리 정보를 찾을 수 없습니다."));

        // Ticket 조회
        Ticket ticket = ticketRepository
                .findById(ticketManagement.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("티켓 정보를 찾을 수 없습니다."));

        return ticket.getTicketType();
    }

    /**
     * TicketOrder를 TicketOrderResponseDto로 변환
     *
     * @param ticketOrder 티켓 주문 엔티티
     * @return 티켓 주문 응답 DTO
     */
    @Transactional(readOnly = true)
    public TicketOrderResponseDto convertToDto(TicketOrder ticketOrder) {
        // TicketManagement 조회
        TicketManagement ticketManagement = ticketManagementRepository
                .findById(ticketOrder.getTicketManagementId())
                .orElseThrow(() -> new IllegalArgumentException("티켓 관리 정보를 찾을 수 없습니다."));

        // Ticket 조회
        Ticket ticket = ticketRepository
                .findById(ticketManagement.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("티켓 정보를 찾을 수 없습니다."));

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
     * 사용자의 모든 티켓 주문을 DTO로 변환하여 조회
     *
     * @param userId 사용자 ID
     * @return 티켓 주문 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<TicketOrderResponseDto> getUserTicketOrdersAsDto(Long userId) {
        List<TicketOrder> ticketOrders = ticketOrderRepository.findByUserId(userId);
        return ticketOrders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
