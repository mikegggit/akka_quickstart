package com.notatracer.akka.experiments;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;


class SupervisingActor extends AbstractActor {
    static Props props() {
        return Props.create(SupervisingActor.class, SupervisingActor::new);
    }

    ActorRef child = null;

    @Override
    public void preStart() throws Exception {
        super.preStart();
        System.out.println("supervising actor started");
        child = getContext().actorOf(SupervisedActor.props(), "supervised-actor");
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        System.out.println("supervising actor stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "failChild",
                        f -> {
                            child.tell("fail", getSelf());
                        })
                .build();
    }
}

class SupervisedActor extends AbstractActor {
    static Props props() {
        return Props.create(SupervisedActor.class, SupervisedActor::new);
    }

    @Override
    public void preStart() {
        System.out.println("supervised actor started");
    }

    @Override
    public void postStop() {
        System.out.println("supervised actor stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "fail",
                        f -> {
                            System.out.println("supervised actor fails now");
                            throw new Exception("I failed!");
                        })
                .build();
    }
}

public class ActorSupervisionExperiments {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("testSystem");

        ActorRef supervisingActor = system.actorOf(SupervisingActor.props(), "supervising-actor");
        supervisingActor.tell("failChild", ActorRef.noSender());
    }
}
