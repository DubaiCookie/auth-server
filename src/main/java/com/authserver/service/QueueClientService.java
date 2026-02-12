package com.authserver.service;

import com.authserver.dto.queue.EnqueueRequest;
import com.authserver.dto.queue.EnqueueResponse;
import com.authserver.dto.queue.QueueStatusListResponse;
import com.authserver.dto.queue.RideQueueInfoListResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 대기열 서버와 통신하는 서비스
 */
@Service
@RequiredArgsConstructor
public class QueueClientService {

    private static final Logger logger = LoggerFactory.getLogger(QueueClientService.class);
    private static final int TIMEOUT_SECONDS = 10;

    private final WebClient queueWebClient;

    /**
     * 대기열 등록 요청을 대기열 서버로 전달
     *
     * @param userId 사용자 ID
     * @param rideId 놀이기구 ID
     * @param ticketType 티켓 타입
     * @return 대기열 등록 응답
     */
    public EnqueueResponse enqueue(Long userId, Long rideId, String ticketType) {
        logger.info("대기열 서버로 등록 요청 전송 - 사용자={}, 놀이기구={}, 티켓타입={}", userId, rideId, ticketType);

        EnqueueRequest request = new EnqueueRequest(userId, rideId, ticketType);

        try {
            EnqueueResponse response = queueWebClient.post()
                    .uri("/api/queue/enqueue")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EnqueueResponse.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();

            if (response == null) {
                throw new RuntimeException("대기열 서버로부터 응답을 받지 못했습니다.");
            }

            logger.info("대기열 서버 응답 - 현재순번={}, 예상대기시간={}분",
                    response.position(), response.estimatedWaitMinutes());

            return response;
        } catch (Exception e) {
            logger.error("대기열 등록 중 오류 발생", e);
            throw new RuntimeException("대기열 서버 통신 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자의 모든 대기열 상태 조회
     *
     * @param userId 사용자 ID
     * @return 대기열 상태 리스트
     */
    public QueueStatusListResponse getAllStatus(Long userId) {
        logger.info("대기열 서버로 전체 상태 조회 요청 - 사용자={}", userId);

        try {
            QueueStatusListResponse response = queueWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/queue/status/all")
                            .queryParam("userId", userId)
                            .build())
                    .retrieve()
                    .bodyToMono(QueueStatusListResponse.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();

            if (response == null) {
                throw new RuntimeException("대기열 서버로부터 응답을 받지 못했습니다.");
            }

            logger.info("대기열 서버 응답 - 항목수={}", response.items().size());

            return response;
        } catch (Exception e) {
            logger.error("대기열 상태 조회 중 오류 발생", e);
            throw new RuntimeException("대기열 서버 통신 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 모든 놀이기구의 대기열 정보 조회
     *
     * @return 놀이기구별 대기열 정보 리스트
     */
    public RideQueueInfoListResponse getAllRidesQueueInfo() {
        logger.info("대기열 서버로 전체 놀이기구 대기열 정보 조회 요청");

        try {
            RideQueueInfoListResponse response = queueWebClient.get()
                    .uri("/api/queue/rides/info")
                    .retrieve()
                    .bodyToMono(RideQueueInfoListResponse.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();

            if (response == null) {
                throw new RuntimeException("대기열 서버로부터 응답을 받지 못했습니다.");
            }

            logger.info("대기열 서버 응답 - 놀이기구수={}", response.rides().size());

            return response;
        } catch (Exception e) {
            logger.error("놀이기구 대기열 정보 조회 중 오류 발생", e);
            throw new RuntimeException("대기열 서버 통신 오류: " + e.getMessage(), e);
        }
    }
}

