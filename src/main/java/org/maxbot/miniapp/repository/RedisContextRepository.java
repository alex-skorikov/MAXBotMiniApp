package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class RedisContextRepository implements ContextRepository {

    private final RedisTemplate<String, UserContext> redis;

    public RedisContextRepository(RedisTemplate<String, UserContext> redis) {
        this.redis = redis;
    }

    @Override
    public UserContext load(String userId) {
        UserContext ctx = redis.opsForValue().get(userId);
        if (ctx == null) {
            ctx = new UserContext();
            ctx.setUserId(Integer.parseInt(userId));
        }
        return ctx;
    }

    @Override
    public void save(UserContext ctx) {
        redis.opsForValue().set(String.valueOf(ctx.getUserId()), ctx);
    }

    @Override
    public void delete(String userId) {
        redis.delete(userId);
    }
}

