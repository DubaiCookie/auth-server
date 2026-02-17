package com.authserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * pay-server의 결제 완료 이벤트를 수신하여 티켓 주문을 자동 생성하는 서비스
 */
@Service
@RequiredArgsConstructor
public class PaymentEventConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumerService.class);

    private final TicketOrderService ticketOrderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "payment-events", groupId = "auth-server-group")
    public void consumePaymentEvent(String message) {
        logger.info("결제 완료 이벤트 수신: {}", message);

        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            String eventType = jsonNode.get("eventType").asText();
            if (!"PAYMENT_COMPLETED".equals(eventType)) {
                return;
            }

            Long userId = jsonNode.get("userId").asLong();
            Long ticketManagementId = jsonNode.get("ticketManagementId").asLong();

            ticketOrderService.createTicketOrder(userId, ticketManagementId);

            logger.info("티켓 주문 자동 생성 완료 - userId={}, ticketManagementId={}", userId, ticketManagementId);

        } catch (Exception e) {
            logger.error("결제 이벤트 처리 중 오류 발생: {}", message, e);
        }
    }
}
