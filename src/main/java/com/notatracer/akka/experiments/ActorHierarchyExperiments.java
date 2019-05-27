package com.notatracer.akka.experiments;

import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;


class PrintMyActorRefActor extends AbstractActor {
  static Props props() {
    return Props.create(PrintMyActorRefActor.class, PrintMyActorRefActor::new);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .matchEquals(
	"printit",
	p -> {
        ActorContext context = getContext();
        ActorRef parent = context.getParent();
        System.out.println(context.getParent());

        // the parent is the creator actor
        ActorRef secondRef = getContext().actorOf(Props.empty(), "second-actor");
	  System.out.println("Second: " + secondRef);
	})
      .build();
  }
}

public class ActorHierarchyExperiments {
  public static void main(String[] args) throws java.io.IOException {
    ActorSystem system = ActorSystem.create("testSystem");

    // the parent is /user
    ActorRef firstRef = system.actorOf(PrintMyActorRefActor.props(), "first-actor");
    System.out.println("First: " + firstRef);

    // Tell first-actor to printit (no sender ref sent since not expecting a reply)
    firstRef.tell("printit", ActorRef.noSender());
    System.out.println(">>> Press ENTER to exit <<<");
    try {
      System.in.read();
    } finally {
      system.terminate();
    }
  }
}
