package edu.scala.sender.flow;

import edu.scala.sender.client.Client;
import edu.scala.sender.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Flow;

public class EventSubscriber implements Flow.Subscriber<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(EventSubscriber.class);
    private final EventSender sender;
    private Flow.Subscription subscription;

    public EventSubscriber(Client client, Duration timeout) {
        this.sender = new EventSender(client, timeout);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(this.sender.getAvailablePlaces());
    }

    @Override
    public void onNext(Message item) {
        this.sender.send(item);
        this.subscription.request(this.sender.getAvailablePlaces());
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.error("Observed read error in subscriber", throwable);
    }

    @Override
    public void onComplete() {
        this.sender.stop();
    }
}
