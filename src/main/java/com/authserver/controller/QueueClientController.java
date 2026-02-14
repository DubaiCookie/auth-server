package com.authserver.controller;

import com.authserver.dto.queue.CancelRequest;
import com.authserver.dto.queue.CancelResponse;
import com.authserver.dto.queue.CompleteRideRequest;
import com.authserver.dto.queue.CompleteRideResponse;
import com.authserver.dto.queue.EnqueueRequest;
import com.authserver.dto.queue.EnqueueResponse;
import com.authserver.dto.queue.QueueStatusListResponse;
import com.authserver.service.QueueClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * 클라이언트의 대기열 요청을 받아 대기열 서버로 전달하는 컨트롤러
 */
@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
@Tag(
        name = "놀이기구 예약 API",
        description = "놀이기구 예약 요청을 대기열 서버로 중계하는 API"
)
public class QueueClientController {

    private static final Logger logger = LoggerFactory.getLogger(QueueClientController.class);
    private final QueueClientService queueClientService;

    /**
     * 클라이언트로부터 대기열 등록 요청을 받아 대기열 서버로 전달
     */
    @Operation(
            summary = "대기열 등록",
            description = "클라이언트가 놀이기구 대기열에 등록합니다. 서버에서 자동으로 활성 티켓을 찾아 이용 가능 여부를 확인한 후, 대기열 서버로 전달하고 현재 순번과 예상 대기 시간을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "대기열 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnqueueResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검사 실패 또는 티켓 없음)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (본인의 대기열만 등록 가능)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 또는 대기열 서버 통신 오류"
            )
    })
    @PostMapping("/enqueue")
    public ResponseEntity<?> enqueue(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody @Valid EnqueueRequest enqueueRequest) {
        Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
        logger.info("대기열 등록 요청 - 인증된사용자={}, 요청사용자={}, 놀이기구={}, 티켓타입={}",
                authenticatedUserId, enqueueRequest.userId(), enqueueRequest.rideId(),
                enqueueRequest.ticketType());

        try {
            EnqueueResponse response = queueClientService.enqueueWithValidation(
                    authenticatedUserId,
                    enqueueRequest.userId(),
                    enqueueRequest.rideId(),
                    enqueueRequest.ticketType()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("대기열 등록 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("대기열 등록 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("대기열 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 클라이언트로부터 대기열 상태 조회 요청을 받아 대기열 서버로 전달
     */
    @Operation(
            summary = "사용자 모든 대기열 상태 조회",
            description = "사용자가 대기 중인 모든 놀이기구의 현재 순번과 예상 대기 시간을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueueStatusListResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (본인의 대기열만 조회 가능)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 또는 대기열 서버 통신 오류"
            )
    })
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getQueueStatus(
            jakarta.servlet.http.HttpServletRequest request,
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId) {
        Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
        logger.info("대기열 상태 조회 요청 - 인증된사용자={}, 요청사용자={}", authenticatedUserId, userId);

        try {
            QueueStatusListResponse response = queueClientService.getAllStatusWithValidation(
                    authenticatedUserId,
                    userId
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("대기열 상태 조회 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("대기열 상태 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("대기열 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 놀이기구 이용 완료 처리
     * 사용자가 READY 상태를 받고 완료 버튼을 누르면 호출되는 API
     * WAITED 상태를 COMPLETED로 변경
     */
    @Operation(
            summary = "놀이기구 이용 완료 처리",
            description = "사용자가 놀이기구 탑승을 완료하고 완료 버튼을 누르면 호출됩니다. ride_usage 테이블의 WAITED 상태를 COMPLETED로 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "완료 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompleteRideResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (대기 중인 예약이 없음)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (본인의 예약만 완료 처리 가능)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    @PostMapping("/complete")
    public ResponseEntity<?> completeRide(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody @Valid CompleteRideRequest completeRequest) {
        Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
        logger.info("놀이기구 이용 완료 요청 - 인증된사용자={}, 요청사용자={}, 놀이기구={}",
                authenticatedUserId, completeRequest.userId(), completeRequest.rideId());

        try {
            queueClientService.completeRideWithValidation(
                    authenticatedUserId,
                    completeRequest.userId(),
                    completeRequest.rideId()
            );

            CompleteRideResponse response = new CompleteRideResponse(
                    true,
                    "놀이기구 이용이 완료되었습니다.",
                    completeRequest.userId(),
                    completeRequest.rideId()
            );


            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("놀이기구 이용 완료 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("놀이기구 이용 완료 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("놀이기구 이용 완료 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 대기열 예약 취소 처리
     * 사용자가 대기 중인 놀이기구 예약을 취소할 때 호출되는 API
     * WAITED 상태의 예약을 삭제하고 대기열 서버에도 취소 요청
     */
    @Operation(
            summary = "대기열 예약 취소",
            description = "사용자가 대기 중인 놀이기구 예약을 취소합니다. ride_usage 테이블의 WAITED 상태 레코드를 삭제하고 대기열 서버에 취소 요청을 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "취소 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CancelResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (취소할 대기 중인 예약이 없음)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (본인의 예약만 취소 가능)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 또는 대기열 서버 통신 오류"
            )
    })
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelQueue(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody @Valid CancelRequest cancelRequest) {
        Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");
        logger.info("대기열 취소 요청 - 인증된사용자={}, 요청사용자={}, 놀이기구={}",
                authenticatedUserId, cancelRequest.userId(), cancelRequest.rideId());

        try {
            CancelResponse response = queueClientService.cancelWithValidation(
                    authenticatedUserId,
                    cancelRequest.userId(),
                    cancelRequest.rideId()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("대기열 취소 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("대기열 취소 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("대기열 취소 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

