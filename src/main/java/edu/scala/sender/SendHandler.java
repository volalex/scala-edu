package edu.scala.sender;

import edu.scala.sender.client.Client;
import edu.scala.sender.flow.FlowReader;
import edu.scala.sender.flow.EventSubscriber;

import java.time.Duration;

public class SendHandler implements Handler {
    private final Client client;
    private final FlowReader flowReader;
    private final Duration duration;

    public SendHandler(Client client, Duration timeout) {
        this.client = client;
        this.duration = timeout;
        this.flowReader = new FlowReader(client);
    }

    @Override
    public Duration timeout() {
        return this.duration;
    }

    @Override
    public void performOperation() {
        this.flowReader
                .asPublisher()
                .subscribe(new EventSubscriber(this.client, this.duration));
    }
}
