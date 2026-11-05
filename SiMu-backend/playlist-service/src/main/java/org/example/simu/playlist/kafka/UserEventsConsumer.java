package org.example.simu.playlist.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.simu.common.event.UserDeletedEvent;
import org.example.simu.common.kafka.KafkaTopics;
import org.example.simu.playlist.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Keeps the playlist database consistent with the identity database: when an account is
 * soft-deleted, the user's playlists disappear from the library as well.
 * <p>
 * This is a second, independent consumer group, so user-service and playlist-service each get every
 * event without competing for partitions.
 */
@Component
public class UserEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventsConsumer.class);

    private final ObjectMapper objectMapper;
    private final PlaylistService playlistService;

    public UserEventsConsumer(ObjectMapper objectMapper, PlaylistService playlistService) {
        this.objectMapper = objectMapper;
        this.playlistService = playlistService;
    }

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "${spring.kafka.consumer.group-id:playlist-service}")
    public void onEvent(ConsumerRecord<String, String> record) {
        if (!"UserDeleted".equals(eventType(record))) {
            return;
        }
        try {
            UserDeletedEvent event = objectMapper.readValue(record.value(), UserDeletedEvent.class);
            playlistService.softDeleteUserLibrary(event.userId());
        } catch (Exception e) {
            log.error("Failed to delete the library of the deleted user at offset {}", record.offset(), e);
        }
    }

    private String eventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(KafkaTopics.EVENT_TYPE_HEADER);
        return header == null ? "unknown" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
