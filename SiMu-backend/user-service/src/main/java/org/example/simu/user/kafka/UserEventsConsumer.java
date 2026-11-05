package org.example.simu.user.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.simu.common.event.UserDeletedEvent;
import org.example.simu.common.event.UserRegisteredEvent;
import org.example.simu.common.kafka.KafkaTopics;
import org.example.simu.user.service.ProfileProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Consumes the user lifecycle stream produced by auth-service.
 * The event type travels in a header, so one topic can carry several event kinds.
 */
@Component
public class UserEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventsConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProfileProjectionService projectionService;

    public UserEventsConsumer(ObjectMapper objectMapper, ProfileProjectionService projectionService) {
        this.objectMapper = objectMapper;
        this.projectionService = projectionService;
    }

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "${spring.kafka.consumer.group-id:user-service}")
    public void onEvent(ConsumerRecord<String, String> record) {
        String eventType = eventType(record);
        try {
            switch (eventType) {
                case "UserRegistered" -> projectionService.applyRegistered(
                        objectMapper.readValue(record.value(), UserRegisteredEvent.class));
                case "UserDeleted" -> projectionService.applyDeleted(
                        objectMapper.readValue(record.value(), UserDeletedEvent.class));
                default -> log.warn("Unknown user event type '{}' at offset {}", eventType, record.offset());
            }
        } catch (Exception e) {
            // Rethrowing would make the container retry; a malformed event must be visible, not silent.
            log.error("Failed to apply {} event at offset {}", eventType, record.offset(), e);
        }
    }

    private String eventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(KafkaTopics.EVENT_TYPE_HEADER);
        return header == null ? "unknown" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
