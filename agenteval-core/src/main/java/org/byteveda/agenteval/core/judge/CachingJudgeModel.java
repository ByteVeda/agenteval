package org.byteveda.agenteval.core.judge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Decorator that caches judge responses to avoid redundant LLM calls
 * for identical evaluation prompts.
 *
 * <p>Cache keys are the full prompt strings. Cached responses include
 * the score, reason, and token usage from the original call.</p>
 *
 * <p>Thread-safe — safe for use with parallel evaluation via virtual threads.</p>
 *
 * <pre>{@code
 * JudgeModel cached = new CachingJudgeModel(delegate);
 * // Second call with same prompt returns cached result without LLM call
 * JudgeResponse r1 = cached.judge("evaluate...");
 * JudgeResponse r2 = cached.judge("evaluate...");  // cache hit
 * }</pre>
 */
public final class CachingJudgeModel implements JudgeModel {

    private static final Logger LOG = LoggerFactory.getLogger(CachingJudgeModel.class);

    private final JudgeModel delegate;
    private final ConcurrentMap<String, JudgeResponse> cache;

    public CachingJudgeModel(JudgeModel delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public JudgeResponse judge(String prompt) {
        JudgeResponse cached = cache.get(prompt);
        if (cached != null) {
            LOG.debug("Cache hit for judge prompt (length={})", prompt.length());
            return cached;
        }

        LOG.debug("Cache miss for judge prompt (length={})", prompt.length());
        JudgeResponse response = delegate.judge(prompt);
        cache.put(prompt, response);
        return response;
    }

    @Override
    public String modelId() {
        return delegate.modelId();
    }

    /**
     * Returns the number of cached entries.
     */
    public int cacheSize() {
        return cache.size();
    }

    /**
     * Clears all cached judge responses.
     */
    public void clearCache() {
        cache.clear();
    }
}
