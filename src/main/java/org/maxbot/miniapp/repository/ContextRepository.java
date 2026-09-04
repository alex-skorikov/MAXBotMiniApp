package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;

public interface ContextRepository {

    UserContext load(String userId);
    void save(UserContext ctx);
    void delete(String userId);
    void syncUserContext(UserContext ctx, PatentSearchRequest req, PatentSearchResponse resp);
}

