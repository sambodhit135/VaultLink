package com.vaultlink.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * RedisConfig — configures Lettuce connection, RedisTemplate, and
 * a named RedisCacheManager with per-cache TTL settings.
 *
 * Cache name constants are exposed as public static final fields so
 * services can reference them without magic strings.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    // -------------------------------------------------------
    // Cache name constants
    // -------------------------------------------------------

    public static final String DOCUMENTS_CACHE     = "documents";
    public static final String DOCUMENT_CACHE      = "document";
    public static final String EXPIRY_DASHBOARD    = "expiryDashboard";
    public static final String CATEGORIES_CACHE    = "categories";
    public static final String NOTIFICATIONS_CACHE = "notifications";

    // -------------------------------------------------------
    // Properties
    // -------------------------------------------------------

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    // -------------------------------------------------------
    // 1. RedisConnectionFactory — Lettuce
    // -------------------------------------------------------

    /**
     * Creates a Lettuce-backed Redis connection factory using
     * host and port from application.properties.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Initializing Redis connection — host={}, port={}", redisHost, redisPort);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, redisPort);
        log.info("RedisConnectionFactory (Lettuce) created successfully");
        return factory;
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // This allows deserialization of typed objects
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    // -------------------------------------------------------
    // 2. RedisTemplate<String, Object>
    // -------------------------------------------------------

    /**
     * Configures a {@link RedisTemplate} with:
     * <ul>
     *   <li>String keys / hash keys</li>
     *   <li>JSON-serialized values / hash values (via Jackson)</li>
     * </ul>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        // Key serializers — always strings for readability in Redis CLI
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value serializers — JSON for type-safe deserialization
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        log.info("RedisTemplate configured with StringRedisSerializer (keys) + GenericJackson2JsonRedisSerializer (values)");
        return template;
    }

    // -------------------------------------------------------
    // 3. CacheManager — per-cache TTL configuration
    // -------------------------------------------------------

    /**
     * Creates a {@link RedisCacheManager} with a 10-minute default TTL
     * and per-cache overrides:
     *
     * <ul>
     *   <li>{@code documents}       — 10 min</li>
     *   <li>{@code document}        — 10 min</li>
     *   <li>{@code expiryDashboard} —  5 min</li>
     *   <li>{@code categories}      — 30 min</li>
     *   <li>{@code notifications}   —  5 min</li>
     * </ul>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        // Base configuration shared by all caches
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                .disableCachingNullValues();

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                DOCUMENTS_CACHE,     defaultConfig.entryTtl(Duration.ofMinutes(10)),
                DOCUMENT_CACHE,      defaultConfig.entryTtl(Duration.ofMinutes(10)),
                EXPIRY_DASHBOARD,    defaultConfig.entryTtl(Duration.ofMinutes(5)),
                CATEGORIES_CACHE,    defaultConfig.entryTtl(Duration.ofMinutes(30)),
                NOTIFICATIONS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5))
        );

        CacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

        log.info("RedisCacheManager configured — caches: {}, {}, {}, {}, {}",
                DOCUMENTS_CACHE, DOCUMENT_CACHE, EXPIRY_DASHBOARD,
                CATEGORIES_CACHE, NOTIFICATIONS_CACHE);

        return cacheManager;
    }
}
