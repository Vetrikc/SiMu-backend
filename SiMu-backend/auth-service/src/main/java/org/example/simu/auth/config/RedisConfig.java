package org.example.simu.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.simu.auth.dto.UserDetailsResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache for user lookups (pattern taken from the internship project).
 * <p>
 * The value serializer is bound to {@link UserDetailsResponse}, so no polymorphic typing metadata
 * has to be written into Redis and a compromised cache cannot instantiate arbitrary classes.
 */
@Configuration
public class RedisConfig {

    public static final String USER_CACHE = "auth:users";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<UserDetailsResponse> values =
                new Jackson2JsonRedisSerializer<>(objectMapper, UserDetailsResponse.class);

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .prefixCacheNameWith("simu:")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(values));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
