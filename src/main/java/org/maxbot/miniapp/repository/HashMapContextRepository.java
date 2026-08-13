package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class HashMapContextRepository implements ContextRepository {

    private final Map<String, UserContext> storage = new ConcurrentHashMap<>();

    @Override
    public UserContext load(String chatId) {
        UserContext ctx = storage.get(chatId);
        if (ctx == null) {
            ctx = new UserContext();
            ctx.setUserId(Integer.parseInt(chatId));
        }
        return ctx;
    }

    @Override
    public void save(UserContext ctx) {
        storage.put(String.valueOf(ctx.getUserId()), ctx);
    }

    @Override
    public void delete(String chatId) {
        storage.remove(chatId);
    }

    // для очистки хранилища между тестами
    public void clear() {
        storage.clear();
    }
}
