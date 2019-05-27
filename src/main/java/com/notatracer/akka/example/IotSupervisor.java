package com.notatracer.akka.example;

import akka.actor.AbstractActor;
import akka.actor.ActorLogging;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class IotSupervisor extends AbstractActor {
    // Use Akka's built-in logging functionality
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {   // Recommended way to create an actor...
        return Props.create(IotSupervisor.class, IotSupervisor::new);
    }

    @Override
    public void preStart() {
        log.info("IoT Application started");
    }

    @Override
    public void postStop() {
        log.info("IoT Application stopped");
    }

    // No need to handle any messages
    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }
}
