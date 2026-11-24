package com.racelab.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis configuration for distributed locking.
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getHost());
        config.setPort(properties.getPort());
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            config.setPassword(properties.getPassword());
        }
        if (properties.getDatabase() != null) {
            config.setDatabase(properties.getDatabase());
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Data
    public static class RedisProperties {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private Integer database = 0;
        private int timeout = 2000;
        private Lettuce lettuce = new Lettuce();

        @Data
        public static class Lettuce {
            private Pool pool = new Pool();
            private ShutdownTimeout shutdownTimeout = 100;

            @Data
            public static class Pool {
                private int maxActive = 8;
                private int maxIdle = 8;
                private int minIdle = 0;
            }
        }
    }
}