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
import com.authserver.entity.TicketManagement;
import com.authserver.service.TicketManagementService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ticket-management")
@RequiredArgsConstructor
@Tag(name = "티켓 재고 관리 API", description = "티켓 재고 관리 관련 API")
public class TicketManagementController {

    private final TicketManagementService ticketManagementService;

    /**
     * POST /api/ticket-management - 티켓 재고 생성
     */
    @Operation(summary = "티켓 재고 생성", description = "새로운 티켓 재고를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<TicketManagement> createTicketManagement(
            @Parameter(description = "티켓 ID", required = true, example = "1")
            @RequestParam Long ticketId,
            @Parameter(description = "판매 가능 일시 (ISO 8601)", required = true, example = "2026-02-20T10:00:00")
            @RequestParam String availableAt,
            @Parameter(description = "재고 수량", required = true, example = "100")
            @RequestParam Integer stock) {
        try {
            LocalDateTime availableAtDateTime = LocalDateTime.parse(availableAt);
            TicketManagement ticketManagement = ticketManagementService.createTicketManagement(
                    ticketId, availableAtDateTime, stock);
            return ResponseEntity.status(HttpStatus.CREATED).body(ticketManagement);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ticket-management/{ticketManagementId} - 특정 티켓 재고 조회
     */
    @Operation(summary = "티켓 재고 조회", description = "ID로 특정 티켓 재고를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "티켓 재고를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{ticketManagementId}")
    public ResponseEntity<TicketManagement> getTicketManagement(
            @Parameter(description = "티켓 재고 ID", required = true)
            @PathVariable Long ticketManagementId) {
        try {
            TicketManagement ticketManagement = ticketManagementService.getTicketManagement(ticketManagementId);
            return ResponseEntity.ok(ticketManagement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ticket-management - 모든 티켓 재고 조회
     */
    @Operation(summary = "전체 티켓 재고 조회", description = "등록된 모든 티켓 재고를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<TicketManagement>> getAllTicketManagements() {
        try {
            List<TicketManagement> ticketManagements = ticketManagementService.getAllTicketManagements();
            return ResponseEntity.ok(ticketManagements);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ticket-management/ticket/{ticketId} - 특정 티켓의 재고 조회
     */
    @Operation(summary = "특정 티켓의 재고 조회", description = "티켓 ID로 재고를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<TicketManagement>> getTicketManagementsByTicketId(
            @Parameter(description = "티켓 ID", required = true)
            @PathVariable Long ticketId) {
        try {
            List<TicketManagement> ticketManagements = 
                    ticketManagementService.getTicketManagementsByTicketId(ticketId);
            return ResponseEntity.ok(ticketManagements);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ticket-management/available - 판매 가능한 티켓 재고 조회
     */
    @Operation(summary = "판매 가능한 티켓 재고 조회", description = "특정 날짜 이후 판매 가능한 티켓 재고를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/available")
    public ResponseEntity<List<TicketManagement>> getAvailableTicketManagements(
            @Parameter(description = "조회 시작 일시 (ISO 8601)", required = true, example = "2026-02-20T10:00:00")
            @RequestParam String dateTime) {
        try {
            LocalDateTime dateTimeParam = LocalDateTime.parse(dateTime);
            List<TicketManagement> ticketManagements = 
                    ticketManagementService.getAvailableTicketManagements(dateTimeParam);
            return ResponseEntity.ok(ticketManagements);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ticket-management/in-stock - 재고가 있는 티켓 조회
     */
    @Operation(summary = "재고가 있는 티켓 조회", description = "최소 재고량 이상인 티켓을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/in-stock")
    public ResponseEntity<List<TicketManagement>> getInStockTicketManagements(
            @Parameter(description = "최소 재고량", required = false, example = "0")
            @RequestParam(defaultValue = "0") Integer minStock) {
        try {
            List<TicketManagement> ticketManagements = 
                    ticketManagementService.getInStockTicketManagements(minStock);
            return ResponseEntity.ok(ticketManagements);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/ticket-management/{ticketManagementId} - 티켓 재고 정보 수정
     */
    @Operation(summary = "티켓 재고 수정", description = "티켓 재고 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "티켓 재고를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/{ticketManagementId}")
    public ResponseEntity<TicketManagement> updateTicketManagement(
            @Parameter(description = "티켓 재고 ID", required = true)
            @PathVariable Long ticketManagementId,
            @Parameter(description = "티켓 ID", example = "1")
            @RequestParam(required = false) Long ticketId,
            @Parameter(description = "판매 가능 일시 (ISO 8601)", example = "2026-02-20T10:00:00")
            @RequestParam(required = false) String availableAt,
            @Parameter(description = "재고 수량", example = "50")
            @RequestParam(required = false) Integer stock) {
        try {
            LocalDateTime availableAtDateTime = availableAt != null ? LocalDateTime.parse(availableAt) : null;
            TicketManagement ticketManagement = ticketManagementService.updateTicketManagement(
                    ticketManagementId, ticketId, availableAtDateTime, stock);
            return ResponseEntity.ok(ticketManagement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/ticket-management/{ticketManagementId} - 티켓 재고 삭제
     */
    @Operation(summary = "티켓 재고 삭제", description = "티켓 재고를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{ticketManagementId}")
    public ResponseEntity<Void> deleteTicketManagement(
            @Parameter(description = "티켓 재고 ID", required = true)
            @PathVariable Long ticketManagementId) {
        try {
            ticketManagementService.deleteTicketManagement(ticketManagementId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
