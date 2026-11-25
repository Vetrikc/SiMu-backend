package org.example.simu.auth.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.TopicDescription;
import org.example.simu.common.kafka.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Declares the topic with enough partitions for consumer parallelism and upgrades an existing
 * single-partition topic in place.
 */
@Configuration
public class KafkaTopicConfig {

    public static final int PARTITIONS = 6;
    private static final Logger log = LoggerFactory.getLogger(KafkaTopicConfig.class);

    @Bean
    public org.apache.kafka.clients.admin.NewTopic userEvents() {
        return TopicBuilder.name(KafkaTopics.USER_EVENTS).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic userEventsDlt() {
        return TopicBuilder.name(KafkaTopics.USER_EVENTS + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public ApplicationRunner topicPartitionUpgrader(KafkaAdmin kafkaAdmin) {
        return (ApplicationArguments args) -> {
            try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                Map<String, TopicDescription> described = client
                        .describeTopics(List.of(KafkaTopics.USER_EVENTS))
                        .allTopicNames().get(10, TimeUnit.SECONDS);
                TopicDescription topic = described.get(KafkaTopics.USER_EVENTS);
                if (topic != null && topic.partitions().size() < PARTITIONS) {
                    client.createPartitions(Map.of(KafkaTopics.USER_EVENTS, NewPartitions.increaseTo(PARTITIONS)))
                            .all().get(10, TimeUnit.SECONDS);
                    log.info("Increased partitions of {} from {} to {}",
                            KafkaTopics.USER_EVENTS, topic.partitions().size(), PARTITIONS);
                }
            } catch (Exception e) {
                log.warn("Could not ensure topic partitions: {}", e.getMessage());
            }
        };
    }
}
