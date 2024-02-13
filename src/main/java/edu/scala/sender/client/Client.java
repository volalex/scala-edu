package edu.scala.sender.client;

public interface Client {
    Event readData();

    Result sendData(Address dest, Payload payload);
}
