package edu.scala.sender.model;

import edu.scala.sender.client.Address;
import edu.scala.sender.client.Payload;

public record Message(Address address, Payload payload) {
}
