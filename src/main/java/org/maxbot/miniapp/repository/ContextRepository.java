package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;

public interface ContextRepository {
    UserContext load(String userId);
    void save(UserContext ctx);
}

