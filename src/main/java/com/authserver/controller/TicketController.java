package com.authserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.authserver.dto.TicketOrderResponseDto;
import com.authserver.entity.TicketOrder;
import com.authserver.entity.ActiveStatus;
import com.authserver.entity.TicketType;
import com.authserver.service.TicketOrderService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "티켓 API", description = "티켓 주문 관리 관련 API")
public class TicketController {

    private final TicketOrderService ticketOrderService;

    /**
     * POST /api/tickets - 티켓 주문 생성
     */
    @Operation(summary = "티켓 주문", description = "이용 날짜와 티켓 타입으로 새로운 티켓을 주문합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<TicketOrder> createTicketOrder(
            jakarta.servlet.http.HttpServletRequest request,
            @Parameter(description = "이용 날짜 (ISO 8601)", required = true, example = "2026-02-20T10:00:00")
            @RequestParam String availableAt,
            @Parameter(description = "티켓 타입 (GENERAL 또는 PREMIUM)", required = true, example = "GENERAL")
            @RequestParam TicketType ticketType) {
        try {
            Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
            LocalDateTime availableAtDateTime = LocalDateTime.parse(availableAt);
            TicketOrder ticketOrder = ticketOrderService.createTicketOrderByDateAndType(
                authenticatedUserId, availableAtDateTime, ticketType);
            return ResponseEntity.status(HttpStatus.CREATED).body(ticketOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/tickets/my - 내 오늘 이후 티켓 주문 조회
     */
    @Operation(summary = "내 오늘 이후 티켓 주문 조회", description = "인증된 사용자의 오늘 포함 이후 티켓 주문을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my")
    public ResponseEntity<List<TicketOrderResponseDto>> getMyTicketOrders(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
            List<TicketOrderResponseDto> ticketOrders = ticketOrderService.getUserTicketOrdersDto(authenticatedUserId);
            return ResponseEntity.ok(ticketOrders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/tickets/my/active - 내 활성 티켓 주문 조회
     */
    @Operation(summary = "내 활성 티켓 주문 조회", description = "인증된 사용자의 오늘 포함 이후 활성 상태 티켓 주문만 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my/active")
    public ResponseEntity<List<TicketOrderResponseDto>> getMyActiveTicketOrders(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
            List<TicketOrderResponseDto> ticketOrders = ticketOrderService.getUserActiveTicketOrdersDto(authenticatedUserId);
            return ResponseEntity.ok(ticketOrders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/tickets/{ticketOrderId}/status - 티켓 주문 상태 변경
     */
    @Operation(summary = "티켓 주문 상태 변경", description = "티켓 주문의 상태를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "티켓 주문을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{ticketOrderId}/status")
    public ResponseEntity<TicketOrder> updateTicketOrderStatus(
            @Parameter(description = "티켓 주문 ID", required = true)
            @PathVariable Long ticketOrderId,
            @Parameter(description = "변경할 상태", required = true, example = "ACTIVE")
            @RequestParam ActiveStatus status) {
        try {
            TicketOrder ticketOrder = ticketOrderService.updateTicketOrderStatus(ticketOrderId, status);
            return ResponseEntity.ok(ticketOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/tickets/{ticketOrderId} - 티켓 주문 삭제
     */
    @Operation(summary = "티켓 주문 삭제", description = "티켓 주문을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{ticketOrderId}")
    public ResponseEntity<Void> deleteTicketOrder(
            @Parameter(description = "티켓 주문 ID", required = true)
            @PathVariable Long ticketOrderId) {
        try {
            ticketOrderService.deleteTicketOrder(ticketOrderId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
