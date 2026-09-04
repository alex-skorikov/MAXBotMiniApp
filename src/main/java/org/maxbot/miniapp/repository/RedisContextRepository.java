package org.maxbot.miniapp.repository;

import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("!test")
public class RedisContextRepository implements ContextRepository {

    private final RedisTemplate<String, UserContext> redis;
    private static final Logger log = LoggerFactory.getLogger(RedisContextRepository.class);

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

    @Override
    public void syncUserContext(UserContext ctx, PatentSearchRequest req, PatentSearchResponse resp) {
        String actualQuery = req.getQuery();
        if (actualQuery != null && !actualQuery.isBlank()) {
            ctx.setSearchQuery(actualQuery);
        }
        ctx.setLimit(req.getLimit() > 0 ? req.getLimit() : 5);
        ctx.setOffset(req.getOffset());

        if (req.getDatasets() != null && !req.getDatasets().isEmpty()) {
            ctx.setDatasetArrays(req.getDatasets());
        }

        if (req.getFilter() != null) {
            if (req.getFilter().getDatePublished() != null &&
                    req.getFilter().getDatePublished().getRange() != null) {
                ctx.setDate(req.getFilter().getDatePublished().getRange().getGt());
            }

            if (req.getFilter().getClassification() != null &&
                    req.getFilter().getClassification().getValues() != null &&
                    !req.getFilter().getClassification().getValues().isEmpty()) {
                ctx.setClassifiers(req.getFilter().getClassification().getValues().get(0));
            }
        }

        if (resp != null && resp.getHits() != null && !resp.getHits().isEmpty()) {
            ctx.setHits(resp.getHits());
            log.info("💾 [SYNC] В контекст пользователя {} кэшировано документов: {}", ctx.getUserId(), resp.getHits()
                    .size());
        } else {
            ctx.setHits(List.of()); // Очищаем старый кэш hits, если Роспатент вернул 0 результатов
            log.info("🗑️ [SYNC] По запросу '{}' документов не найдено. Кэш hits пользователя {} очищен.", actualQuery, ctx.getUserId());
        }
    }
}

