package com.agenteval.core.cost;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;

import java.util.Objects;

/**
 * Decorator that wraps a {@link JudgeModel} and records cost on each call.
 */
public final class CostTrackingJudgeModel implements JudgeModel {

    private final JudgeModel delegate;
    private final CostTracker tracker;
    private final PricingModel pricing;

    public CostTrackingJudgeModel(JudgeModel delegate, CostTracker tracker,
                                  PricingModel pricing) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.tracker = Objects.requireNonNull(tracker, "tracker must not be null");
        this.pricing = Objects.requireNonNull(pricing, "pricing must not be null");
    }

    @Override
    public JudgeResponse judge(String prompt) {
        JudgeResponse response = delegate.judge(prompt);
        tracker.record(response.tokenUsage(), pricing);
        return response;
    }

    @Override
    public String modelId() {
        return delegate.modelId();
    }

    /**
     * Returns the underlying cost tracker.
     */
    public CostTracker tracker() {
        return tracker;
    }
}
