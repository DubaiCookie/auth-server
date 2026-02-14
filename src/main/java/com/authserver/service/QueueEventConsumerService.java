package com.authserver.service;

import com.authserver.dto.websocket.QueueEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka로부터 대기열 이벤트를 수신하여 WebSocket으로 전달하는 서비스
 */
@Service
@RequiredArgsConstructor
public class QueueEventConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(QueueEventConsumerService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kafka로부터 대기열 이벤트 수신
     *
     * @param message 카프카 메시지 (JSON 형식)
     */
    @KafkaListener(topics = "queue-event-topic", groupId = "main-server-group")
    public void consumeQueueEvent(String message) {
        logger.info("Kafka 메시지 수신: {}", message);

        try {
            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(message);

            Long rideId = jsonNode.get("rideId").asLong();
            Long userId = jsonNode.get("userId").asLong();
            String type = jsonNode.get("type").asText();
            String status = jsonNode.get("status").asText();

            QueueEventMessage eventMessage = new QueueEventMessage(rideId, userId, type, status);

            // 특정 사용자에게만 메시지 전송
            sendToUser(userId, eventMessage);

            logger.info("웹소켓 메시지 전송 완료 - 사용자={}, 놀이기구={}, 상태={}",
                    userId, rideId, status);

        } catch (Exception e) {
            logger.error("Kafka 메시지 처리 중 오류 발생: {}", message, e);
        }
    }

    /**
     * 특정 사용자에게 웹소켓 메시지 전송
     * Kafka 이벤트 (READY/ALMOST_READY)를 사용자별 대기열 상태 채널로 전송
     *
     * @param userId 사용자 ID
     * @param message 전송할 메시지
     */
    private void sendToUser(Long userId, QueueEventMessage message) {
        // 사용자별 대기열 상태와 동일한 채널로 전송
        // /sub/user/{userId}/queue-status
        String destination = "/sub/user/" + userId + "/queue-status";
        messagingTemplate.convertAndSend(destination, message);

        logger.info("웹소켓 탑승 알림 전송 - destination={}, rideId={}, status={}",
                destination, message.rideId(), message.status());
    }
}

