package edu.scala.sender.client;

import java.util.List;

public record Event(List<Address> recipients, Payload payload) {
}
