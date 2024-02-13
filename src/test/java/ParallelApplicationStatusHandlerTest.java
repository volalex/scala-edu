import edu.scala.appstatus.ParallelApplicationStatusHandler;
import edu.scala.appstatus.client.Client;
import edu.scala.appstatus.client.Response;
import edu.scala.appstatus.model.ApplicationStatusResponse;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParallelApplicationStatusHandlerTest {
    @Mock
    Client client;
    @InjectMocks
    ParallelApplicationStatusHandler handler;

    @Test
    @Timeout(value = 15100, unit = TimeUnit.MILLISECONDS)
    void timeoutTest() {
        when(client.getApplicationStatus2(any())).thenAnswer(new AnswersWithDelay(16000, new Returns(success())));
        when(client.getApplicationStatus1(any())).thenAnswer(new AnswersWithDelay(16000, new Returns(success())));
        var result = handler.performOperation("123");
        assertThat(result)
                .isInstanceOfSatisfying(ApplicationStatusResponse.Failure.class, (f) -> {
                    assertThat(f.retriesCount()).isZero();
                    assertThat(f.lastRequestTime()).isNull();
                });
    }

    @Test
    @Timeout(value = 3100, unit = TimeUnit.MILLISECONDS)
    void failureTest() {
        when(client.getApplicationStatus2(any())).thenAnswer(new AnswersWithDelay(3000, new Returns(fail())));
        when(client.getApplicationStatus1(any())).thenAnswer(new AnswersWithDelay(5000, new Returns(fail())));
        var result = handler.performOperation("123");
        assertThat(result).isInstanceOfSatisfying(ApplicationStatusResponse.Failure.class, (f) -> {
            assertThat(f.retriesCount()).isZero();
            assertThat(f.lastRequestTime()).isCloseTo(Duration.ofMillis(3000), Duration.ofMillis(100));
        });
    }

    @Test
    @Timeout(value = 3100, unit = TimeUnit.MILLISECONDS)
    void successTest() {
        when(client.getApplicationStatus1(any())).thenAnswer(new AnswersWithDelay(5000, new Returns(success())));
        when(client.getApplicationStatus2(any())).thenAnswer(new AnswersWithDelay(3000, new Returns(success())));
        var result = handler.performOperation("123");
        assertThat(result).isInstanceOfSatisfying(ApplicationStatusResponse.Success.class, s -> {
            assertThat(s.status()).isEqualTo("STATUS");
            assertThat(s.id()).isEqualTo("4i5023i50235029304");
        });
    }

    @Test
    @Timeout(value = 15100, unit = TimeUnit.MILLISECONDS)
    void retryTest() {
        when(client.getApplicationStatus1(any())).thenAnswer(new AnswersWithDelay(1000, new Returns(retry())));
        when(client.getApplicationStatus2(any())).thenAnswer(new AnswersWithDelay(1000, new Returns(retry())));
        var result = handler.performOperation("123");
        assertThat(result).isInstanceOfSatisfying(ApplicationStatusResponse.Failure.class, s -> {
            assertThat(s.retriesCount()).isCloseTo(25, Offset.offset(5));
        });
    }


    private Response success() {
        return new Response.Success("STATUS", "4i5023i50235029304");
    }

    private Response fail() {
        return new Response.Failure(new RuntimeException());
    }

    private Response retry() {
        return new Response.RetryAfter(Duration.ofMillis(100));
    }
}
