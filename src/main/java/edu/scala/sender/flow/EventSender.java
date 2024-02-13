package edu.scala.sender.flow;

import edu.scala.sender.client.Client;
import edu.scala.sender.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.scala.sender.client.Result;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EventSender {
    private static final Logger LOG = LoggerFactory.getLogger(EventSender.class);
    private final AtomicInteger parallelism = new AtomicInteger(Runtime.getRuntime().availableProcessors());
    private final ExecutorService executor = Executors.newWorkStealingPool(parallelism.get());
    private final Client client;
    private final Duration timeout;

    public EventSender(Client client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    public void send(Message message) {
        internalSend(message, this.executor);
    }

    private CompletableFuture<Result> internalSend(Message message, Executor taskExecutor) {
        return CompletableFuture
                .supplyAsync(() -> {
                    parallelism.incrementAndGet();
                    return client.sendData(message.address(), message.payload());
                }, taskExecutor)
                .thenComposeAsync(result -> {
                    if (Result.REJECTED.equals(result)) {
                        var delay = CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS, taskExecutor);
                        return this.internalSend(message, delay);
                    } else {
                        return CompletableFuture.completedFuture(result);
                    }
                }, executor)
                .handleAsync((result, throwable) -> {
                    if (throwable != null) {
                        LOG.error("Failed to send message", throwable);
                    }
                    return result;
                }, executor);
    }

    //If sender can accept and process more events - it sends greater number,
    // if not, it sends 1 to handle backpressure
    public int getAvailablePlaces() {
        return parallelism.get();
    }

    public void stop() {
        this.executor.shutdown();
    }
}
