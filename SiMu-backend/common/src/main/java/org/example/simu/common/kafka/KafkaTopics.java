package org.example.simu.common.kafka;

public final class KafkaTopics {

    /** Lifecycle events of a user, published by auth-service and consumed by the other services. */
    public static final String USER_EVENTS = "simu.user.events";

    /** Header that carries the event type, so several event kinds can share one topic. */
    public static final String EVENT_TYPE_HEADER = "eventType";

    private KafkaTopics() {
    }
}
