package com.notatracer.akka.example;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceGroupQuery extends AbstractActor {
    public static final class CollectionTimeout {}

    /*
    getContext() returns context for the current actor, and exposes access to a variety of
    akka facilities.
     */
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    final Map<ActorRef, String> actorToDeviceId;
    final long requestId;
    final ActorRef requester;

    Cancellable queryTimeoutTimer;

    public DeviceGroupQuery(
            Map<ActorRef, String> actorToDeviceId,
            long requestId,
            ActorRef requester,
            FiniteDuration timeout) {
        this.actorToDeviceId = actorToDeviceId;
        this.requestId = requestId;
        this.requester = requester;

        queryTimeoutTimer =
                getContext()
                        .getSystem()
                        .scheduler()
                        .scheduleOnce(
                                timeout,
                                getSelf(),
                                new CollectionTimeout(),
                                getContext().getDispatcher(),
                                getSelf());
    }

    public static Props props(
            Map<ActorRef, String> actorToDeviceId,
            long requestId,
            ActorRef requester,
            FiniteDuration timeout) {
        return Props.create(
                DeviceGroupQuery.class,
                () -> new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout));
    }

    @Override
    public void preStart() {
        for (ActorRef deviceActor : actorToDeviceId.keySet()) {
            getContext().watch(deviceActor);
            deviceActor.tell(new Device.ReadTemperature(0L), getSelf());
        }
    }

    @Override
    public void postStop() {
        queryTimeoutTimer.cancel();
    }

    @Override
    public Receive createReceive() {
        return waitingForReplies(new HashMap<>(), actorToDeviceId.keySet());
    }

    /**
     * Creates a stateful receiver function that tracks responses to TemperatureReading requests
     * sent to a snapshot of device actors registered to the DeviceGroup at the time this actor
     * was created.
     * @param repliesSoFar
     * @param stillWaiting snapshot of the actors registered for the associated DeviceGroup actor.
     * @return A receive function that dictates how the actor behaves on receiving various message types.
     */
    public Receive waitingForReplies(
            Map<String, DeviceGroup.TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting) {
        // state is maintained as part of the function, not as part of the actor.
        return receiveBuilder()
                .match(
                        Device.RespondTemperature.class,
                        r -> {
                            ActorRef deviceActor = getSender();
                            DeviceGroup.TemperatureReading reading =
                                    r.value
                                            .map(v -> (DeviceGroup.TemperatureReading) new DeviceGroup.Temperature(v))
                                            .orElse(DeviceGroup.TemperatureNotAvailable.INSTANCE);
                            receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar);
                        })
                .match(
                        Terminated.class,
                        t -> {
                            receivedResponse(
                                    t.getActor(),
                                    DeviceGroup.DeviceNotAvailable.INSTANCE,
                                    stillWaiting,
                                    repliesSoFar);
                        })
                .match(
                        CollectionTimeout.class,
                        t -> {
                            Map<String, DeviceGroup.TemperatureReading> replies = new HashMap<>(repliesSoFar);
                            for (ActorRef deviceActor : stillWaiting) {
                                String deviceId = actorToDeviceId.get(deviceActor);
                                replies.put(deviceId, DeviceGroup.DeviceTimedOut.INSTANCE);
                            }
                            requester.tell(new DeviceGroup.RespondAllTemperatures(requestId, replies), getSelf());
                            getContext().stop(getSelf());
                        })
                .build();
    }

    public void receivedResponse(
            ActorRef deviceActor,
            DeviceGroup.TemperatureReading reading,
            Set<ActorRef> stillWaiting,
            Map<String, DeviceGroup.TemperatureReading> repliesSoFar) {
        // stop watching the sender of received response to avoid receipt of a Termination event
        // after having already dealt w/ this deviceActor
        getContext().unwatch(deviceActor);
        String deviceId = actorToDeviceId.get(deviceActor);

        // Use immutable model to track state.
        Set<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
        newStillWaiting.remove(deviceActor);

        Map<String, DeviceGroup.TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);
        newRepliesSoFar.put(deviceId, reading);

        if (newStillWaiting.isEmpty()) {
            // All devices have been accounted for, so reply to the original requestor...
            requester.tell(new DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar), getSelf());
            // ...and stop the actor.
            getContext().stop(getSelf());
        } else {
            // swap in a new receive function to handle this actor's behavior going forward.
            getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
        }
    }
}