package com.authserver.controller;

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
        name = "클라이언트 대기열 API",
        description = "클라이언트의 대기열 요청을 대기열 서버로 중계하는 API"
)
public class QueueClientController {

    private static final Logger logger = LoggerFactory.getLogger(QueueClientController.class);
    private final QueueClientService queueClientService;

    /**
     * 클라이언트로부터 대기열 등록 요청을 받아 대기열 서버로 전달
     */
    @Operation(
            summary = "대기열 등록",
            description = "클라이언트가 놀이기구 대기열에 등록합니다. 요청을 대기열 서버로 전달하고 현재 순번과 예상 대기 시간을 반환합니다."
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
                    description = "잘못된 요청 (유효성 검사 실패)"
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
        logger.info("클라이언트 대기열 등록 요청 - 인증된사용자={}, 요청사용자={}, 놀이기구={}, 티켓타입={}",
                authenticatedUserId, enqueueRequest.userId(), enqueueRequest.rideId(), enqueueRequest.ticketType());

        try {
            // 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
            if (!authenticatedUserId.equals(enqueueRequest.userId())) {
                logger.warn("인증된 사용자와 요청 사용자 불일치 - 인증={}, 요청={}", authenticatedUserId, enqueueRequest.userId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("본인의 대기열만 등록할 수 있습니다.");
            }

            EnqueueResponse response = queueClientService.enqueue(
                    enqueueRequest.userId(),
                    enqueueRequest.rideId(),
                    enqueueRequest.ticketType()
            );

            logger.info("클라이언트에게 대기열 등록 응답 전송 - 현재순번={}, 예상대기시간={}분",
                    response.position(), response.estimatedWaitMinutes());

            return ResponseEntity.ok(response);
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
        logger.info("클라이언트 대기열 상태 조회 요청 - 인증된사용자={}, 요청사용자={}", authenticatedUserId, userId);

        try {
            // 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
            if (!authenticatedUserId.equals(userId)) {
                logger.warn("인증된 사용자와 요청 사용자 불일치 - 인증={}, 요청={}", authenticatedUserId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("본인의 대기열 상태만 조회할 수 있습니다.");
            }

            QueueStatusListResponse response = queueClientService.getAllStatus(userId);

            logger.info("클라이언트에게 대기열 상태 응답 전송 - 항목수={}", response.items().size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("대기열 상태 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("대기열 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

