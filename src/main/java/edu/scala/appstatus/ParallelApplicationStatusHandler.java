package edu.scala.appstatus;

import edu.scala.appstatus.client.Client;
import edu.scala.appstatus.client.Response;
import edu.scala.appstatus.model.ApplicationStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ParallelApplicationStatusHandler implements Handler {
    private static final Logger LOG = LoggerFactory.getLogger(ParallelApplicationStatusHandler.class);
    private final Client appStatusClient;
    private final Executor executor;

    public ParallelApplicationStatusHandler(Client appStatusClient, Executor executor) {
        this.appStatusClient = appStatusClient;
        this.executor = Objects.requireNonNullElseGet(executor,
                () -> Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors()));
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        RequestStatsHolder statsHolder = new RequestStatsHolder();
        var future = requestAppStatus(() -> appStatusClient.getApplicationStatus1(id), executor, statsHolder);
        var future2 = requestAppStatus(() -> appStatusClient.getApplicationStatus2(id), executor, statsHolder);
        var composite = future.applyToEitherAsync(future2, Function.identity());
        try {
            return composite.get(15, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to read status for unknown reason", e);
            return fail(statsHolder);
        } catch (TimeoutException e) {
            return fail(statsHolder);
        }
    }

    private CompletableFuture<ApplicationStatusResponse> requestAppStatus(Supplier<Response> responseSupplier,
                                                                          Executor executor,
                                                                          RequestStatsHolder statsHolder) {
        return CompletableFuture.supplyAsync(supplyTimed(responseSupplier)).thenComposeAsync(timedResponse -> {
            if (timedResponse.result instanceof Response.Success success) {
                return CompletableFuture.completedFuture(new ApplicationStatusResponse.Success(success.applicationId(), success.applicationStatus()));
            } else if (timedResponse.result instanceof Response.Failure) {
                //Failure handling assumes that if services are synchronized,
                //second and future calls to the same or different service
                //will return the same failure results.
                //So there is no waiting for a second request to complete.
                statsHolder.recordRequest(timedResponse.requestDuration);
                return CompletableFuture.completedFuture(new ApplicationStatusResponse.Failure(timedResponse.requestDuration, statsHolder.getRetriesCount()));
            } else if (timedResponse.result instanceof Response.RetryAfter retryAfter) {
                statsHolder.recordRequest(timedResponse.requestDuration);
                statsHolder.countRetry();
                var delay = CompletableFuture.delayedExecutor(retryAfter.delay().toMillis(), TimeUnit.MILLISECONDS, executor);
                return requestAppStatus(responseSupplier, delay, statsHolder);
            } else {
                throw new RuntimeException("Unknown result type");
            }
        });
    }

    private ApplicationStatusResponse fail(RequestStatsHolder statsHolder) {
        return new ApplicationStatusResponse.Failure(statsHolder.getDuration(), statsHolder.getRetriesCount());
    }

    private Supplier<TimedStatusResponse> supplyTimed(Supplier<Response> responseSupplier) {
        return () -> {
            var start = Instant.now();
            var result = responseSupplier.get();
            return new TimedStatusResponse(Duration.between(start, Instant.now()), result);
        };
    }

    private record TimedStatusResponse(Duration requestDuration, Response result) {
    }
}
