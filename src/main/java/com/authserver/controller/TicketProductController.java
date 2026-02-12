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
import com.authserver.entity.Ticket;
import com.authserver.entity.TicketType;
import com.authserver.service.TicketService;

import java.util.List;

@RestController
@RequestMapping("/tickets/products")
@RequiredArgsConstructor
@Tag(name = "티켓 상품 API", description = "티켓 상품 관리 관련 API")
public class TicketProductController {

    private final TicketService ticketService;

    /**
     * POST /api/tickets/products - 티켓 상품 생성
     */
    @Operation(summary = "티켓 상품 생성", description = "새로운 티켓 상품을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @Parameter(description = "티켓 타입", required = true, example = "GENERAL")
            @RequestParam TicketType ticketType,
            @Parameter(description = "티켓 개수", required = true, example = "5")
            @RequestParam Integer ticketCount,
            @Parameter(description = "가격", required = true, example = "50000")
            @RequestParam Integer price) {
        try {
            Ticket ticket = ticketService.createTicket(ticketType, ticketCount, price);
            return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/tickets/products/{ticketId} - 특정 티켓 상품 조회
     */
    @Operation(summary = "티켓 상품 조회", description = "ID로 특정 티켓 상품을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "티켓 상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{ticketId}")
    public ResponseEntity<Ticket> getTicket(
            @Parameter(description = "티켓 ID", required = true)
            @PathVariable Long ticketId) {
        try {
            Ticket ticket = ticketService.getTicket(ticketId);
            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/tickets/products - 모든 티켓 상품 조회
     */
    @Operation(summary = "전체 티켓 상품 조회", description = "등록된 모든 티켓 상품을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        try {
            List<Ticket> tickets = ticketService.getAllTickets();
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/tickets/products/type/{ticketType} - 티켓 타입으로 조회
     */
    @Operation(summary = "티켓 타입별 조회", description = "티켓 타입으로 상품을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/type/{ticketType}")
    public ResponseEntity<List<Ticket>> getTicketsByType(
            @Parameter(description = "티켓 타입", required = true, example = "GENERAL")
            @PathVariable TicketType ticketType) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByType(ticketType);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/tickets/products/{ticketId} - 티켓 상품 정보 수정
     */
    @Operation(summary = "티켓 상품 수정", description = "티켓 상품 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "티켓 상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/{ticketId}")
    public ResponseEntity<Ticket> updateTicket(
            @Parameter(description = "티켓 ID", required = true)
            @PathVariable Long ticketId,
            @Parameter(description = "티켓 타입", example = "PREMIUM")
            @RequestParam(required = false) TicketType ticketType,
            @Parameter(description = "티켓 개수", example = "10")
            @RequestParam(required = false) Integer ticketCount,
            @Parameter(description = "가격", example = "100000")
            @RequestParam(required = false) Integer price) {
        try {
            Ticket ticket = ticketService.updateTicket(ticketId, ticketType, ticketCount, price);
            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/tickets/products/{ticketId} - 티켓 상품 삭제
     */
    @Operation(summary = "티켓 상품 삭제", description = "티켓 상품을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(
            @Parameter(description = "티켓 ID", required = true)
            @PathVariable Long ticketId) {
        try {
            ticketService.deleteTicket(ticketId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
