package org.example.back.llm.service;

import org.example.back.config.ProjectAssistantProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LlmGovernanceService {

    private final ProjectAssistantProperties properties;
    private final Map<Long, Deque<Long>> userRequestWindows = new ConcurrentHashMap<>();
    private final Map<String, DailyBudgetState> modelBudgetStates = new ConcurrentHashMap<>();

    public LlmGovernanceService(ProjectAssistantProperties properties) {
        this.properties = properties;
    }

    public RateLimitDecision checkUserRateLimit(Long userId) {
        if (userId == null) {
            return RateLimitDecision.allowed();
        }

        int minuteLimit = Math.max(0, properties.getLlm().getRateLimit().getUserPerMinute());
        int hourLimit = Math.max(0, properties.getLlm().getRateLimit().getUserPerHour());
        if (minuteLimit == 0 && hourLimit == 0) {
            return RateLimitDecision.allowed();
        }

        long now = System.currentTimeMillis();
        Deque<Long> requests = userRequestWindows.computeIfAbsent(userId, ignored -> new ArrayDeque<>());

        synchronized (requests) {
            pruneExpiredRequests(requests, now);

            if (minuteLimit > 0) {
                int minuteCount = countWithinWindow(requests, now, 60_000L);
                if (minuteCount >= minuteLimit) {
                    long retryAfterSeconds = computeRetryAfterSeconds(requests, now, 60_000L);
                    return RateLimitDecision.blocked(retryAfterSeconds);
                }
            }

            if (hourLimit > 0) {
                int hourCount = countWithinWindow(requests, now, 3_600_000L);
                if (hourCount >= hourLimit) {
                    long retryAfterSeconds = computeRetryAfterSeconds(requests, now, 3_600_000L);
                    return RateLimitDecision.blocked(retryAfterSeconds);
                }
            }

            requests.addLast(now);
            return RateLimitDecision.allowed();
        }
    }

    public boolean tryConsumeModelBudget(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return true;
        }

        ProjectAssistantProperties.BudgetConfig budgetConfig = properties.getLlm().getBudget().get(modelCode);
        if (budgetConfig == null || budgetConfig.getDailyRequests() <= 0) {
            return true;
        }

        DailyBudgetState state = modelBudgetStates.computeIfAbsent(modelCode, ignored -> new DailyBudgetState(LocalDate.now(), 0));
        synchronized (state) {
            LocalDate today = LocalDate.now();
            if (!today.equals(state.day)) {
                state.day = today;
                state.used = 0;
            }

            if (state.used >= budgetConfig.getDailyRequests()) {
                return false;
            }

            state.used++;
            return true;
        }
    }

    private void pruneExpiredRequests(Deque<Long> requests, long now) {
        long earliestAllowed = now - 3_600_000L;
        while (!requests.isEmpty() && requests.peekFirst() < earliestAllowed) {
            requests.pollFirst();
        }
    }

    private int countWithinWindow(Deque<Long> requests, long now, long windowMillis) {
        long earliestAllowed = now - windowMillis;
        int count = 0;
        for (Long timestamp : requests) {
            if (timestamp >= earliestAllowed) {
                count++;
            }
        }
        return count;
    }

    private long computeRetryAfterSeconds(Deque<Long> requests, long now, long windowMillis) {
        long earliestAllowed = now - windowMillis;
        for (Long timestamp : requests) {
            if (timestamp >= earliestAllowed) {
                long waitMillis = timestamp + windowMillis - now;
                return Math.max(1L, (long) Math.ceil(waitMillis / 1000.0));
            }
        }
        return 1L;
    }

    public static class RateLimitDecision {
        private final boolean allowed;
        private final long retryAfterSeconds;

        private RateLimitDecision(boolean allowed, long retryAfterSeconds) {
            this.allowed = allowed;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static RateLimitDecision allowed() {
            return new RateLimitDecision(true, 0L);
        }

        public static RateLimitDecision blocked(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    private static class DailyBudgetState {
        private LocalDate day;
        private int used;

        private DailyBudgetState(LocalDate day, int used) {
            this.day = day;
            this.used = used;
        }
    }
}