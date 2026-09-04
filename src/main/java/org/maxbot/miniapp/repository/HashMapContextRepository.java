package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("test")
public class HashMapContextRepository implements ContextRepository {

    private final Map<String, UserContext> storage = new ConcurrentHashMap<>();

    @Override
    public UserContext load(String userId) {
        UserContext ctx = storage.get(userId);
        if (ctx == null) {
            ctx = new UserContext();
            ctx.setUserId(Integer.parseInt(userId));
        }
        return ctx;
    }

    @Override
    public void save(UserContext ctx) {
        storage.put(String.valueOf(ctx.getUserId()), ctx);
    }

    @Override
    public void delete(String userId) {
        storage.remove(userId);
    }

    @Override
    public void syncUserContext(UserContext ctx, PatentSearchRequest req, PatentSearchResponse resp) {
    }

    public UserContext storageGetDirectly(String userId) {
        return storage.get(userId);
    }

    // для очистки хранилища между тестами
    public void clear() {
        storage.clear();
    }
}
