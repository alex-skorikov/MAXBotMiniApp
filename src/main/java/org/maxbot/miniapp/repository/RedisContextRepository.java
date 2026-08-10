package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisContextRepository implements ContextRepository {

    private final RedisTemplate<String, UserContext> redis;

    public RedisContextRepository(RedisTemplate<String, UserContext> redis) {
        this.redis = redis;
    }

    @Override
    public UserContext load(String chatId) {
        UserContext ctx = redis.opsForValue().get(chatId);
        if (ctx == null) {
            ctx = new UserContext();
            ctx.setUserId(Integer.parseInt(chatId));
        }
        return ctx;
    }

    @Override
    public void save(UserContext ctx) {
        redis.opsForValue().set(String.valueOf(ctx.getUserId()), ctx);
    }

    @Override
    public void delete(String chatId) {
        redis.delete(chatId);
    }
}

