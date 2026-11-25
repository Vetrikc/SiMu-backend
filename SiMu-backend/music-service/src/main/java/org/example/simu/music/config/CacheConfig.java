package org.example.simu.music.config;

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
 * Read-through cache for the catalogue. Pages and search results are cached for 30 seconds:
 * long enough to absorb the read traffic, short enough to stay correct after the eviction on write.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MUSIC_PAGE = "music:page";
    public static final String MUSIC_SEARCH = "music:search";

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
                .withCacheConfiguration(MUSIC_PAGE, defaults.entryTtl(Duration.ofSeconds(30)))
                .withCacheConfiguration(MUSIC_SEARCH, defaults.entryTtl(Duration.ofSeconds(30)))
                .build();
    }
}