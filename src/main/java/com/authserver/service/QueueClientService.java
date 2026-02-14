package com.authserver.service;

import com.authserver.dto.queue.CancelResponse;
import com.authserver.dto.queue.EnqueueRequest;
import com.authserver.dto.queue.EnqueueResponse;
import com.authserver.dto.queue.QueueStatusListResponse;
import com.authserver.dto.queue.RideQueueInfoListResponse;
import com.authserver.entity.Ride;
import com.authserver.entity.TicketOrder;
import com.authserver.repository.RideRepository;
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
    private final TicketOrderService ticketOrderService;
    private final RideUsageService rideUsageService;
    private final RideRepository rideRepository;

    /**
     * 대기열 등록 요청을 검증하고 대기열 서버로 전달
     *
     * 비즈니스 로직:
     * 1. 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
     * 2. 사용자의 오늘 날짜 활성 티켓 찾기 (ACTIVE 상태 + available_at이 오늘)
     * 3. 티켓 존재 여부 확인
     * 4. 요청한 티켓 타입이 실제 티켓 타입과 일치하는지 확인
     * 5. 놀이기구 예약 가능 여부 확인
     * 6. 대기열 서버로 등록 요청
     * 7. RideUsage 테이블에 WAITING 상태로 기록
     *
     * @param authenticatedUserId 인증된 사용자 ID
     * @param userId 요청 사용자 ID
     * @param rideId 놀이기구 ID
     * @param ticketType 티켓 타입 (GENERAL 또는 PREMIUM)
     * @return 대기열 등록 응답
     * @throws IllegalArgumentException 검증 실패 시
     * @throws RuntimeException 대기열 서버 통신 오류 시
     */
    public EnqueueResponse enqueueWithValidation(
            Long authenticatedUserId,
            Long userId,
            Long rideId,
            String ticketType) {

        logger.info("대기열 등록 검증 시작 - 인증된사용자={}, 요청사용자={}, 놀이기구={}, 요청티켓타입={}",
                authenticatedUserId, userId, rideId, ticketType);

        // 1. 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
        if (!authenticatedUserId.equals(userId)) {
            logger.warn("인증된 사용자와 요청 사용자 불일치 - 인증={}, 요청={}", authenticatedUserId, userId);
            throw new IllegalArgumentException("본인의 대기열만 등록할 수 있습니다.");
        }

        // 2. 사용자의 오늘 날짜 활성 티켓 찾기 (ACTIVE 상태 + available_at이 오늘인 티켓)
        TicketOrder ticketOrder = ticketOrderService.getTodayActiveTicket(userId)
                .orElseThrow(() -> {
                    logger.warn("오늘 날짜 활성 티켓이 없음 - userId={}", userId);
                    return new IllegalArgumentException("오늘 날짜에 사용 가능한 티켓이 없습니다. 티켓을 먼저 구매해주세요.");
                });

        Long ticketOrderId = ticketOrder.getTicketOrderId();
        Long ticketManagementId = ticketOrder.getTicketManagementId();

        logger.info("오늘 날짜 활성 티켓 찾음 - ticketOrderId={}, ticketManagementId={}, userId={}, activeStatus={}",
                ticketOrderId, ticketManagementId, userId, ticketOrder.getActiveStatus());

        // 3. 티켓의 실제 타입 조회
        com.authserver.entity.TicketType actualTicketType = ticketOrderService.getTicketType(ticketOrder);

        logger.info("티켓 타입 확인 - 실제티켓타입={}, 요청티켓타입={}", actualTicketType, ticketType);

        // 4. 요청한 티켓 타입이 실제 티켓 타입과 일치하는지 확인
        if (!actualTicketType.name().equals(ticketType)) {
            logger.warn("티켓 타입 불일치 - 실제={}, 요청={}", actualTicketType, ticketType);
            throw new IllegalArgumentException(
                    String.format("보유한 티켓 타입(%s)과 요청한 티켓 타입(%s)이 일치하지 않습니다.",
                            actualTicketType.name(), ticketType));
        }

        // 5. 해당 티켓으로 해당 놀이기구를 예약할 수 있는지 확인
        if (!rideUsageService.canEnqueue(ticketOrderId, rideId)) {
            logger.warn("예약 불가 - 이미 이용했거나 대기 중 - ticketOrderId={}, rideId={}",
                    ticketOrderId, rideId);
            throw new IllegalArgumentException("이미 이용했거나 대기 중인 놀이기구입니다. 티켓 하나당 각 놀이기구는 1번만 예약 가능합니다.");
        }

        // 6. 대기열 서버로 등록 요청
        EnqueueResponse response = enqueue(userId, rideId, ticketType, ticketOrderId);

        // 7. RideUsage 테이블에 WAITING 상태로 기록 생성
        rideUsageService.createRideUsage(userId, rideId, ticketOrderId);

        logger.info("대기열 등록 성공 - 사용자={}, 놀이기구={}, 티켓주문ID={}, 티켓타입={}, 현재순번={}, 예상대기시간={}분",
                userId, rideId, ticketOrderId, ticketType, response.position(), response.estimatedWaitMinutes());

        return response;
    }

    /**
     * 대기열 등록 요청을 대기열 서버로 전달 (내부 메서드)
     *
     * Note: ticketOrderId는 메인 서버에서만 사용하고 대기열 서버로는 전달하지 않음
     *
     * @param userId 사용자 ID
     * @param rideId 놀이기구 ID
     * @param ticketType 티켓 타입
     * @param ticketOrderId 티켓 주문 ID (대기열 서버로는 미전송, 로깅용)
     * @return 대기열 등록 응답
     */
    private EnqueueResponse enqueue(Long userId, Long rideId, String ticketType, Long ticketOrderId) {
        logger.info("대기열 서버로 등록 요청 전송 - 사용자={}, 놀이기구={}, 티켓타입={}, 티켓주문ID={}",
                userId, rideId, ticketType, ticketOrderId);

        // 대기열 서버로는 ticketOrderId를 보내지 않음 (대기열 서버는 ticketOrderId를 관리하지 않음)
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
     * 사용자의 모든 대기열 상태 조회 (인증 포함)
     *
     * @param authenticatedUserId 인증된 사용자 ID
     * @param userId 요청 사용자 ID
     * @return 대기열 상태 리스트
     * @throws IllegalArgumentException 인증 실패 시
     * @throws RuntimeException 대기열 서버 통신 오류 시
     */
    public QueueStatusListResponse getAllStatusWithValidation(Long authenticatedUserId, Long userId) {
        logger.info("대기열 상태 조회 검증 시작 - 인증된사용자={}, 요청사용자={}", authenticatedUserId, userId);

        // 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
        if (!authenticatedUserId.equals(userId)) {
            logger.warn("인증된 사용자와 요청 사용자 불일치 - 인증={}, 요청={}", authenticatedUserId, userId);
            throw new IllegalArgumentException("본인의 대기열 상태만 조회할 수 있습니다.");
        }

        return getAllStatus(userId);
    }

    /**
     * 사용자의 모든 대기열 상태 조회 (내부 메서드)
     *
     * @param userId 사용자 ID
     * @return 대기열 상태 리스트
     */
    private QueueStatusListResponse getAllStatus(Long userId) {
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

            // rideName이 null인 경우 rideId로 조회하여 채워넣기
            if (response.items() != null && !response.items().isEmpty()) {
                java.util.List<com.authserver.dto.queue.QueueStatusItem> updatedItems =
                        response.items().stream()
                                .map(item -> {
                                    String rideName = item.rideName();
                                    if (rideName == null || rideName.isEmpty()) {
                                        try {
                                            Ride ride = rideRepository.findById(item.rideId())
                                                    .orElseThrow(() -> new IllegalArgumentException("놀이기구를 찾을 수 없습니다."));
                                            rideName = ride.getName();
                                            logger.debug("놀이기구 이름 조회 완료 - rideId={}, rideName={}", item.rideId(), rideName);
                                        } catch (Exception e) {
                                            logger.warn("놀이기구 이름 조회 실패 - rideId={}", item.rideId(), e);
                                            rideName = "Unknown";
                                        }
                                    }
                                    return new com.authserver.dto.queue.QueueStatusItem(
                                            item.rideId(),
                                            rideName,
                                            item.ticketType(),
                                            item.position(),
                                            item.estimatedWaitMinutes()
                                    );
                                })
                                .collect(java.util.stream.Collectors.toList());

                return new QueueStatusListResponse(updatedItems);
            }

            return response;
        } catch (Exception e) {
            logger.error("대기열 상태 조회 중 오류 발생", e);
            throw new RuntimeException("대기열 서버 통신 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자의 모든 대기열 상태 조회 (WebSocket용 공개 메서드)
     *
     * @param userId 사용자 ID
     * @return 대기열 상태 리스트
     */
    public QueueStatusListResponse getUserQueueStatus(Long userId) {
        return getAllStatus(userId);
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

    /**
     * 특정 놀이기구의 대기열 정보 조회
     *
     * @param rideId 놀이기구 ID
     * @return 놀이기구 대기열 정보
     */
    public com.authserver.dto.queue.RideQueueInfoDto getRideQueueInfo(Long rideId) {
        logger.info("대기열 서버로 특정 놀이기구 대기열 정보 조회 요청 - 놀이기구={}", rideId);

        try {
            com.authserver.dto.queue.RideQueueInfoDto response = queueWebClient.get()
                    .uri("/api/queue/rides/{rideId}/info", rideId)
                    .retrieve()
                    .bodyToMono(com.authserver.dto.queue.RideQueueInfoDto.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();

            if (response == null) {
                throw new RuntimeException("대기열 서버로부터 응답을 받지 못했습니다.");
            }

            logger.info("대기열 서버 응답 - 놀이기구={}, 대기열타입수={}", rideId, response.waitTimes().size());

            return response;
        } catch (Exception e) {
            logger.error("특정 놀이기구 대기열 정보 조회 중 오류 발생 - 놀이기구={}", rideId, e);
            throw new RuntimeException("대기열 서버 통신 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 놀이기구 이용 완료 처리 (검증 포함)
     *
     * 비즈니스 로직:
     * 1. 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
     * 2. RideUsageService를 통해 WAITED → COMPLETED 상태 변경
     *
     * @param authenticatedUserId 인증된 사용자 ID
     * @param userId 요청 사용자 ID
     * @param rideId 놀이기구 ID
     * @throws IllegalArgumentException 인증 실패 또는 대기 중인 예약이 없는 경우
     */
    public void completeRideWithValidation(Long authenticatedUserId, Long userId, Long rideId) {
        logger.info("놀이기구 이용 완료 검증 시작 - 인증된사용자={}, 요청사용자={}, 놀이기구={}",
                authenticatedUserId, userId, rideId);

        // 1. 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
        if (!authenticatedUserId.equals(userId)) {
            logger.warn("인증된 사용자와 요청 사용자 불일치 - 인증={}, 요청={}", authenticatedUserId, userId);
            throw new IllegalArgumentException("본인의 예약만 완료 처리할 수 있습니다.");
        }

        // 2. WAITED -> COMPLETED 상태 변경
        rideUsageService.completeRideByUserAndRide(userId, rideId);

        logger.info("놀이기구 이용 완료 처리 성공 - userId={}, rideId={}", userId, rideId);
    }

    /**
     * 대기열 취소 처리 (검증 포함)
     *
     * 비즈니스 로직:
     * 1. 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
     * 2. 사용자의 오늘 날짜 활성 티켓 찾기
     * 3. 티켓의 실제 타입 조회
     * 4. userId와 rideId로 WAITED 상태의 RideUsage가 존재하는지 확인
     * 5. 존재하면 대기열 서버로 취소 요청 전송 (티켓 타입 포함)
     * 6. 대기열 서버 응답 성공 시 RideUsage 삭제
     *
     * @param authenticatedUserId 인증된 사용자 ID
     * @param userId 요청 사용자 ID
     * @param rideId 놀이기구 ID
     * @return 대기열 취소 응답
     * @throws IllegalArgumentException 인증 실패 또는 대기 중인 예약이 없는 경우
     * @throws RuntimeException 대기열 서버 통신 오류 시
     */
    public CancelResponse cancelWithValidation(Long authenticatedUserId, Long userId, Long rideId) {
        logger.info("대기열 취소 검증 시작 - 인증된사용자={}, 요청사용자={}, 놀이기구={}",
                authenticatedUserId, userId, rideId);

        // 1. 인증된 사용자 ID와 요청의 userId가 일치하는지 확인 (보안)
        if (!authenticatedUserId.equals(userId)) {
            logger.warn("인증된 사용자와 요청 사용자 불일치 - 인증={}, 요청={}", authenticatedUserId, userId);
            throw new IllegalArgumentException("본인의 예약만 취소할 수 있습니다.");
        }

        // 2. 사용자의 오늘 날짜 활성 티켓 찾기
        TicketOrder ticketOrder = ticketOrderService.getTodayActiveTicket(userId)
                .orElseThrow(() -> {
                    logger.warn("오늘 날짜 활성 티켓이 없음 - userId={}", userId);
                    return new IllegalArgumentException("오늘 날짜에 사용 가능한 티켓이 없습니다.");
                });

        // 3. 티켓의 실제 타입 조회
        com.authserver.entity.TicketType actualTicketType = ticketOrderService.getTicketType(ticketOrder);
        String ticketType = actualTicketType.name();
        logger.info("사용자 티켓 타입 조회 완료 - userId={}, ticketType={}", userId, ticketType);

        // 4. userId와 rideId로 WAITED 상태의 RideUsage가 존재하는지 확인
        boolean hasWaitedReservation = rideUsageService.hasWaitedReservation(userId, rideId);
        if (!hasWaitedReservation) {
            logger.warn("대기 중인 예약이 없음 - userId={}, rideId={}", userId, rideId);
            throw new IllegalArgumentException("취소할 대기 중인 예약이 없습니다.");
        }

        // 5. 대기열 서버로 취소 요청 전송 (티켓 타입 포함)
        CancelResponse cancelResponse = cancelQueueInServer(userId, rideId, ticketType);

        // 6. 대기열 서버 응답 성공 시 RideUsage 삭제
        if (cancelResponse.success()) {
            rideUsageService.deleteWaitedReservation(userId, rideId);
            logger.info("대기열 취소 처리 완료 - userId={}, rideId={}, ticketType={}", userId, rideId, ticketType);
        } else {
            logger.warn("대기열 서버 취소 실패 - userId={}, rideId={}, ticketType={}, message={}",
                    userId, rideId, ticketType, cancelResponse.message());
            throw new RuntimeException("대기열 취소에 실패했습니다: " + cancelResponse.message());
        }

        return cancelResponse;
    }

    /**
     * 대기열 서버로 취소 요청 전송 (내부 메서드)
     *
     * @param userId 사용자 ID
     * @param rideId 놀이기구 ID
     * @param ticketType 티켓 타입
     * @return 취소 응답
     */
    private CancelResponse cancelQueueInServer(Long userId, Long rideId, String ticketType) {
        logger.info("대기열 서버로 취소 요청 전송 - 사용자={}, 놀이기구={}, 티켓타입={}", userId, rideId, ticketType);

        try {
            // 대기열 서버는 EnqueueRequest 형식으로 받음
            EnqueueRequest cancelRequest = new EnqueueRequest(userId, rideId, ticketType);

            // 대기열 서버는 void를 반환하므로, 성공 시 우리가 CancelResponse를 생성
            queueWebClient.post()
                    .uri("/api/queue/cancel")
                    .bodyValue(cancelRequest)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();

            logger.info("대기열 서버 취소 완료 - 사용자={}, 놀이기구={}, 티켓타입={}", userId, rideId, ticketType);

            return new CancelResponse(true, "예약이 취소되었습니다.", userId, rideId);
        } catch (Exception e) {
            logger.error("대기열 서버 취소 요청 중 오류 발생", e);
            throw new RuntimeException("대기열 서버 통신 오류: " + e.getMessage(), e);
        }
    }
}

