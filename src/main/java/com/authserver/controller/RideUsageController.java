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
import com.authserver.entity.RideUsage;
import com.authserver.entity.RideUsageStatus;
import com.authserver.service.RideUsageService;

import java.util.List;

@RestController
@RequestMapping("/api/ride-usages")
@RequiredArgsConstructor
@Tag(name = "놀이기구 이용 기록 API", description = "놀이기구 이용 기록 관리 API")
public class RideUsageController {

    private final RideUsageService rideUsageService;

    /**
     * POST /api/ride-usages - 이용 기록 생성 (대기 시작)
     */
    @Operation(summary = "이용 기록 생성", description = "놀이기구 이용 기록을 생성합니다 (대기 시작).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<RideUsage> createRideUsage(
            jakarta.servlet.http.HttpServletRequest request,
            @Parameter(description = "놀이기구 ID", required = true, example = "1")
            @RequestParam Long rideId,
            @Parameter(description = "티켓 주문 ID", required = true, example = "1")
            @RequestParam Long ticketOrderId) {
        try {
            Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
            RideUsage rideUsage = rideUsageService.createRideUsage(authenticatedUserId, rideId, ticketOrderId);
            return ResponseEntity.status(HttpStatus.CREATED).body(rideUsage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ride-usages/{rideUsageId} - 특정 이용 기록 조회
     */
    @Operation(summary = "이용 기록 조회", description = "ID로 특정 이용 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "이용 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{rideUsageId}")
    public ResponseEntity<RideUsage> getRideUsage(
            @Parameter(description = "이용 기록 ID", required = true)
            @PathVariable Long rideUsageId) {
        try {
            RideUsage rideUsage = rideUsageService.getRideUsage(rideUsageId);
            return ResponseEntity.ok(rideUsage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ride-usages/my - 내 이용 기록 조회
     */
    @Operation(summary = "내 이용 기록 조회", description = "인증된 사용자의 모든 이용 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my")
    public ResponseEntity<List<RideUsage>> getMyRideUsages(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
            List<RideUsage> rideUsages = rideUsageService.getUserRideUsages(authenticatedUserId);
            return ResponseEntity.ok(rideUsages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ride-usages/ride/{rideId} - 놀이기구별 이용 기록 조회
     */
    @Operation(summary = "놀이기구별 이용 기록 조회", description = "특정 놀이기구의 모든 이용 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<List<RideUsage>> getRideUsagesByRide(
            @Parameter(description = "놀이기구 ID", required = true)
            @PathVariable Long rideId) {
        try {
            List<RideUsage> rideUsages = rideUsageService.getRideUsagesByRide(rideId);
            return ResponseEntity.ok(rideUsages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/ride-usages/status/{status} - 상태별 이용 기록 조회
     */
    @Operation(summary = "상태별 이용 기록 조회", description = "특정 상태의 이용 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<RideUsage>> getRideUsagesByStatus(
            @Parameter(description = "이용 상태", required = true, example = "WAITED")
            @PathVariable RideUsageStatus status) {
        try {
            List<RideUsage> rideUsages = rideUsageService.getRideUsagesByStatus(status);
            return ResponseEntity.ok(rideUsages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/ride-usages/{rideUsageId}/arrived - 입장 가능 알림 시각 기록
     */
    @Operation(summary = "입장 가능 알림", description = "입장 가능 알림 시각을 기록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "이용 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{rideUsageId}/arrived")
    public ResponseEntity<RideUsage> setArrivedAt(
            @Parameter(description = "이용 기록 ID", required = true)
            @PathVariable Long rideUsageId) {
        try {
            RideUsage rideUsage = rideUsageService.setArrivedAt(rideUsageId);
            return ResponseEntity.ok(rideUsage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/ride-usages/{rideUsageId}/complete - 이용 완료 처리
     */
    @Operation(summary = "이용 완료 처리", description = "놀이기구 이용을 완료 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "이용 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{rideUsageId}/complete")
    public ResponseEntity<RideUsage> completeRideUsage(
            @Parameter(description = "이용 기록 ID", required = true)
            @PathVariable Long rideUsageId) {
        try {
            RideUsage rideUsage = rideUsageService.completeRideUsage(rideUsageId);
            return ResponseEntity.ok(rideUsage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/ride-usages/{rideUsageId}/no-show - 노쇼 처리
     */
    @Operation(summary = "노쇼 처리", description = "이용 기록을 노쇼 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "이용 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{rideUsageId}/no-show")
    public ResponseEntity<RideUsage> markAsNoShow(
            @Parameter(description = "이용 기록 ID", required = true)
            @PathVariable Long rideUsageId) {
        try {
            RideUsage rideUsage = rideUsageService.markAsNoShow(rideUsageId);
            return ResponseEntity.ok(rideUsage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/ride-usages/{rideUsageId} - 이용 기록 삭제
     */
    @Operation(summary = "이용 기록 삭제", description = "이용 기록을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{rideUsageId}")
    public ResponseEntity<Void> deleteRideUsage(
            @Parameter(description = "이용 기록 ID", required = true)
            @PathVariable Long rideUsageId) {
        try {
            rideUsageService.deleteRideUsage(rideUsageId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
