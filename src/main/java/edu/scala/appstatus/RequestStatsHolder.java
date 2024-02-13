package edu.scala.appstatus;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RequestStatsHolder {
    private final AtomicInteger retriesCount = new AtomicInteger(0);
    private final AtomicReference<Duration> lastRequestTime = new AtomicReference<>();

    public void recordRequest(Duration duration) {
        lastRequestTime.set(duration);
    }

    public void countRetry() {
        retriesCount.incrementAndGet();
    }

    public int getRetriesCount() {
        return retriesCount.get();
    }

    public Duration getDuration() {
        return lastRequestTime.get();
    }
}
