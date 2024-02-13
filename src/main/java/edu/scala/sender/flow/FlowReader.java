package edu.scala.sender.flow;

import edu.scala.sender.model.Message;
import edu.scala.sender.client.Client;
import edu.scala.sender.client.Event;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlowReader {
    private final SubmissionPublisher<Message> flowPublisher;
    private final Thread eventReaderThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public FlowReader(Client client) {
        flowPublisher = new SubmissionPublisher<>();
        eventReaderThread = new Thread(() -> {
            while (isRunning.get()) {
                Event data = client.readData();
                data.recipients().stream()
                        .map(a -> new Message(a, data.payload())).forEach(flowPublisher::submit);
            }
        }, "event-reader-thread");
    }

    public void start() {
        this.eventReaderThread.start();
    }

    public void stop() {
        isRunning.compareAndSet(true, false);
        flowPublisher.close();
    }

    public Flow.Publisher<Message> asPublisher() {
        return flowPublisher;
    }
}
