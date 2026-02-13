package com.authserver.dto;

import com.authserver.entity.ActiveStatus;
import com.authserver.entity.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 티켓 주문 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketOrderResponseDto {

    private Long ticketOrderId;
    private Long userId;
    private LocalDateTime availableAt;
    private TicketType ticketType;
    private LocalDateTime paymentDate;
    private ActiveStatus activeStatus;
}
