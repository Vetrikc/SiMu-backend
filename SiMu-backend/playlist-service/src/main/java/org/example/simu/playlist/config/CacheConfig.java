package org.example.simu.playlist.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Library of a user is read on every screen open, so it is cached for 30 seconds and evicted on
 * any write. Keys carry the user id, so tenants never see each other's data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PLAYLIST_LIBRARY = "playlist:library";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))
                .disableCachingNullValues()
                // JDK serialization: values are typed, so records with generic lists round-trip safely
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.java()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration(PLAYLIST_LIBRARY, defaults.entryTtl(Duration.ofSeconds(30)))
                .build();
    }
}