package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;

public interface ContextRepository {

    UserContext load(String chatId);
    void save(UserContext ctx);
    void delete(String chatId);
}

