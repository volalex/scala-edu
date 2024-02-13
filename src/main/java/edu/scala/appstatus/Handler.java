package edu.scala.appstatus;

import edu.scala.appstatus.model.ApplicationStatusResponse;

public interface Handler {
    ApplicationStatusResponse performOperation(String id);
}
