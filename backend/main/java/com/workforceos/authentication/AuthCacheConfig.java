package com.workforceos.authentication;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
class AuthCacheConfig {

    @Bean
    @SuppressWarnings("unchecked")
    RedisCacheManagerBuilderCustomizer userAccountCacheCustomizer(ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<UserAccount> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, UserAccount.class);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        (RedisSerializer<Object>) (RedisSerializer<?>) serializer));
        return builder -> builder.withCacheConfiguration("userAccount", config);
    }
}
