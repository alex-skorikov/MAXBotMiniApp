package org.maxbot.miniapp.config;

import org.maxbot.miniapp.core.UserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, UserContext> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, UserContext> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Ключи будут сохраняться как обычные строки
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<UserContext> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(UserContext.class);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
