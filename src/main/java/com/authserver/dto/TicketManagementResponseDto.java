package com.authserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.authserver.entity.TicketType;

import java.time.LocalDateTime;

/**
 * 티켓 재고 관리 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketManagementResponseDto {
    private Long ticketManagementId;
    private TicketType ticketType;
    private LocalDateTime availableAt;
    private Integer stock;
}
