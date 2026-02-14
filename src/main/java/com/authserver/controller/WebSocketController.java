package com.authserver.controller;

import com.authserver.service.WebSocketSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 연결을 관리하는 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Tag(name = "WebSocket API", description = "대기열 이벤트를 위한 WebSocket 연결")
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    private final WebSocketSchedulerService schedulerService;

    /**
     * 클라이언트가 전체 놀이기구 대기 시간을 구독할 때 호출
     * 경로: /pub/rides/minutes
     * 수신: /sub/rides/minutes
     *
     * 1분마다 자동으로 전체 놀이기구의 대기 시간이 브로드캐스트됨
     */
    @MessageMapping("/rides/minutes")
    @Operation(summary = "전체 놀이기구 대기 시간 구독", description = "모든 놀이기구의 대기 시간을 1분마다 받습니다.")
    public void subscribeAllRidesMinutes(Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";
        logger.info("WebSocket 전체 놀이기구 대기 시간 구독 - Principal={}", username);
        // 구독만 처리, 데이터는 스케줄러에서 자동으로 /sub/rides/minutes로 브로드캐스트
    }

    /**
     * 클라이언트가 특정 놀이기구의 상세 대기열 정보를 구독할 때 호출
     * 경로: /pub/rides/{rideId}/info
     * 수신: /sub/rides/{rideId}/info
     *
     * 초기 데이터는 REST API(GET /rides/{rideId})로 받고,
     * 이후 1분마다 자동으로 업데이트된 대기열 정보가 브로드캐스트됨
     */
    @MessageMapping("/rides/{rideId}/info")
    @Operation(summary = "놀이기구 상세 대기열 정보 구독", description = "특정 놀이기구의 프리미엄/일반 대기열 정보를 1분마다 받습니다.")
    public void subscribeRideDetailInfo(@DestinationVariable("rideId") Long rideId, Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";
        logger.info("WebSocket 놀이기구 상세 대기열 정보 구독 - rideId={}, Principal={}", rideId, username);
        // 구독만 처리, 데이터는 스케줄러에서 자동으로 /sub/rides/{rideId}/info로 브로드캐스트
    }

    /**
     * 클라이언트가 자신의 대기열 상태를 구독할 때 호출
     * 경로: /pub/user/{userId}/queue-status
     * 수신: /sub/user/{userId}/queue-status
     *
     * 이 채널로 Kafka로부터 받은 READY/ALMOST_READY 탑승 알림이 실시간으로 전송됨
     * 초기 대기열 상태는 별도 REST API로 조회
     */
    @MessageMapping("/user/{userId}/queue-status")
    @Operation(summary = "사용자별 대기열 알림 구독",
               description = "사용자가 예약한 놀이기구의 탑승 알림(READY/ALMOST_READY)을 실시간으로 받습니다.")
    public void subscribeUserQueueStatus(@DestinationVariable("userId") Long userId, Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";
        logger.info("WebSocket 사용자 대기열 알림 구독 - userId={}, Principal={}", userId, username);
        // 구독만 처리, Kafka 이벤트가 이 채널로 전송됨
    }
}

