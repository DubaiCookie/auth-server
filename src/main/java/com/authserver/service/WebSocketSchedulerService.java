package com.authserver.service;

import com.authserver.dto.queue.RideQueueInfoListResponse;
import com.authserver.dto.queue.QueueStatusListResponse;
import com.authserver.dto.websocket.AllRidesMinutesEvent;
import com.authserver.dto.websocket.RideDetailQueueInfo;
import com.authserver.dto.websocket.UserQueueStatusEvent;
import com.authserver.entity.Ride;
import com.authserver.entity.RideUsage;
import com.authserver.entity.RideUsageStatus;
import com.authserver.repository.RideRepository;
import com.authserver.repository.RideUsageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;


import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket을 통해 주기적으로 대기열 정보를 브로드캐스트하는 서비스
 */
@Service
@RequiredArgsConstructor
public class WebSocketSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSchedulerService.class);

    private final QueueClientService queueClientService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RideRepository rideRepository;
    private final RideUsageRepository rideUsageRepository;

    /**
     * 1분마다 전체 놀이기구의 대기 시간을 조회하여 브로드캐스트
     * 목적지: /sub/rides/minutes
     */
    @Scheduled(fixedRate = 60000)
    public void fetchAndBroadcastAllRidesMinutes() {
        try {
            logger.debug("전체 놀이기구 대기 시간 조회 시작");

            RideQueueInfoListResponse response = queueClientService.getAllRidesQueueInfo();

            if (response != null && response.rides() != null) {
                List<AllRidesMinutesEvent.RideMinutes> rides = response.rides().stream()
                        .map(ride -> {
                            // waitTimes에서 최소 대기 시간 추출 (PREMIUM이 일반적으로 더 짧음)
                            int minWaitMinutes = ride.waitTimes().stream()
                                    .mapToInt(wt -> wt.estimatedWaitMinutes())
                                    .min()
                                    .orElse(0);
                            return new AllRidesMinutesEvent.RideMinutes(
                                    ride.rideId(),
                                    minWaitMinutes
                            );
                        })
                        .collect(Collectors.toList());

                AllRidesMinutesEvent event = new AllRidesMinutesEvent(rides);

                messagingTemplate.convertAndSend("/sub/rides/minutes", event);

                logger.info("전체 놀이기구 대기 시간 브로드캐스트 완료 - 놀이기구 수={}", rides.size());
            }
        } catch (Exception e) {
            logger.error("전체 놀이기구 대기 시간 브로드캐스트 실패", e);
        }
    }

    /**
     * 특정 놀이기구의 상세 대기열 정보를 브로드캐스트
     * 목적지: /sub/rides/{rideId}/info
     *
     * @param rideId 놀이기구 ID
     */
    public void broadcastRideDetailInfo(Long rideId) {
        try {
            logger.debug("놀이기구 상세 대기열 정보 조회 시작 - rideId={}", rideId);

            com.authserver.dto.queue.RideQueueInfoDto response = queueClientService.getRideQueueInfo(rideId);

            if (response != null && response.waitTimes() != null) {
                List<RideDetailQueueInfo.WaitTime> waitTimes = response.waitTimes().stream()
                        .map(wt -> new RideDetailQueueInfo.WaitTime(
                                wt.ticketType(),
                                wt.waitingCount(),
                                wt.estimatedWaitMinutes()
                        ))
                        .collect(Collectors.toList());

                RideDetailQueueInfo event = new RideDetailQueueInfo(rideId, waitTimes);

                messagingTemplate.convertAndSend("/sub/rides/" + rideId + "/info", event);

                logger.debug("놀이기구 상세 대기열 정보 브로드캐스트 완료 - rideId={}", rideId);
            }
        } catch (Exception e) {
            logger.error("놀이기구 상세 대기열 정보 브로드캐스트 실패 - rideId={}", rideId, e);
        }
    }

    /**
     * 1분마다 모든 활성 놀이기구의 상세 대기열 정보를 각 채널로 브로드캐스트
     */
    @Scheduled(fixedRate = 60000)
    public void scheduledRideDetailBroadcast() {
        try {
            logger.debug("모든 활성 놀이기구 상세 정보 브로드캐스트 시작");

            // 활성화된 모든 놀이기구 조회
            List<Ride> activeRides = rideRepository.findByIsActive(true);

            if (activeRides.isEmpty()) {
                logger.debug("활성화된 놀이기구 없음");
                return;
            }

            logger.info("활성 놀이기구 상세 정보 브로드캐스트 - 놀이기구 수={}", activeRides.size());

            // 각 놀이기구에 대해 상세 정보 브로드캐스트
            activeRides.forEach(ride -> broadcastRideDetailInfo(ride.getRideId()));

        } catch (Exception e) {
            logger.error("활성 놀이기구 상세 정보 브로드캐스트 실패", e);
        }
    }

    /**
     * 특정 사용자의 모든 대기열 상태를 1분마다 브로드캐스트
     * 목적지: /sub/user/{userId}/queue-status
     *
     * @param userId 사용자 ID
     */
    public void broadcastUserQueueStatus(Long userId) {
        try {
            logger.debug("사용자 대기열 상태 조회 시작 - userId={}", userId);

            QueueStatusListResponse response = queueClientService.getUserQueueStatus(userId);

            if (response != null && response.items() != null) {
                List<UserQueueStatusEvent.QueueItem> items = response.items().stream()
                        .map(item -> {
                            // rideName이 null인 경우 rideId로 조회
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
                            
                            return new UserQueueStatusEvent.QueueItem(
                                    item.rideId(),
                                    rideName,
                                    item.ticketType(),
                                    item.position().intValue(),
                                    item.estimatedWaitMinutes()
                            );
                        })
                        .collect(Collectors.toList());

                UserQueueStatusEvent event = new UserQueueStatusEvent(userId, items);

                messagingTemplate.convertAndSend("/sub/user/" + userId + "/queue-status", event);

                logger.debug("사용자 대기열 상태 브로드캐스트 완료 - userId={}, 대기열 수={}", userId, items.size());
            }
        } catch (Exception e) {
            logger.error("사용자 대기열 상태 브로드캐스트 실패 - userId={}", userId, e);
        }
    }

    /**
     * 1분마다 대기 중인 사용자들의 대기열 상태를 브로드캐스트
     * WAITED 상태인 사용자들을 DB에서 조회하고, 각 사용자별로 대기열 서버에 요청하여 전송
     */
    @Scheduled(fixedRate = 60000)
    public void scheduledUserQueueStatusBroadcast() {
        try {
            logger.debug("사용자 대기열 상태 스케줄러 시작");

            // WAITED 상태인 모든 RideUsage 조회
            List<RideUsage> waitedUsages = rideUsageRepository.findByStatus(RideUsageStatus.WAITED);

            if (waitedUsages.isEmpty()) {
                logger.debug("대기 중인 사용자 없음");
                return;
            }

            // 고유한 userId 목록 추출
            List<Long> uniqueUserIds = waitedUsages.stream()
                    .map(RideUsage::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            logger.info("대기 중인 사용자 대기열 상태 브로드캐스트 시작 - 사용자 수={}", uniqueUserIds.size());

            // 각 사용자별로 대기열 상태 조회 및 브로드캐스트
            uniqueUserIds.forEach(this::broadcastUserQueueStatus);

            logger.info("대기 중인 사용자 대기열 상태 브로드캐스트 완료 - 사용자 수={}", uniqueUserIds.size());

        } catch (Exception e) {
            logger.error("사용자 대기열 상태 브로드캐스트 실패", e);
        }
    }
}


