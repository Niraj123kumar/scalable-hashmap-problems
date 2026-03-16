import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

class TokenBucket {
    private final long maxTokens;
    private final long refillRatePerSecond;
    private AtomicLong tokens;
    private volatile long lastRefillTimestamp;

    TokenBucket(long maxTokens, long refillRatePerHour) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerHour / 3600;
        this.tokens = new AtomicLong(maxTokens);
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    synchronized boolean allowRequest() {
        refill();
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        } else {
            return false;
        }
    }

    synchronized long tokensRemaining() {
        refill();
        return tokens.get();
    }

    synchronized long refill() {
        long now = System.currentTimeMillis();
        long secondsSinceLast = (now - lastRefillTimestamp) / 1000;
        if (secondsSinceLast > 0) {
            long refillTokens = secondsSinceLast * refillRatePerSecond;
            long newTokens = Math.min(maxTokens, tokens.get() + refillTokens);
            tokens.set(newTokens);
            lastRefillTimestamp = now;
        }
        return tokens.get();
    }
}

public class DistributedRateLimiter {

    private final ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();
    private final long requestsPerHour;

    public DistributedRateLimiter(long requestsPerHour) {
        this.requestsPerHour = requestsPerHour;
    }

    public String checkRateLimit(String clientId) {
        TokenBucket bucket = clientBuckets.computeIfAbsent(clientId, k -> new TokenBucket(requestsPerHour, requestsPerHour));
        boolean allowed = bucket.allowRequest();
        if (allowed) {
            return "Allowed (" + bucket.tokensRemaining() + " requests remaining)";
        } else {
            long secondsUntilReset = 3600 - ((System.currentTimeMillis() / 1000) % 3600);
            return "Denied (0 requests remaining, retry after " + secondsUntilReset + "s)";
        }
    }

    public String getRateLimitStatus(String clientId) {
        TokenBucket bucket = clientBuckets.computeIfAbsent(clientId, k -> new TokenBucket(requestsPerHour, requestsPerHour));
        long used = requestsPerHour - bucket.tokensRemaining();
        long reset = ((System.currentTimeMillis() / 1000) / 3600 + 1) * 3600;
        return "{used: " + used + ", limit: " + requestsPerHour + ", reset: " + reset + "}";
    }

    public static void main(String[] args) throws InterruptedException {
        DistributedRateLimiter limiter = new DistributedRateLimiter(1000);

        System.out.println(limiter.checkRateLimit("abc123"));
        System.out.println(limiter.checkRateLimit("abc123"));
        System.out.println(limiter.getRateLimitStatus("abc123"));
    }
}
