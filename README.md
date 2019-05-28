Akka Quickstart
===============


Best practices
--------------
Be a pessimistic as makes sense.
Assume any message can be lost for a given transmission attempt.
Favor courser actors if in doubt, go finer if the need arises.


Use cases
---------
Register a device with the system
Record a temperature reading.
Respond to a request for the last temperature reading.


Actors
------
# DeviceManager 
Handles registration of devices with the system.
Associates a device w/ an actor responsible for maintaining state associated w/ sensor readings so the system can make use of the data provided by the sensor.
Registration request will include both a group id and device id.
If an actor associated w/ the group id doesn't exist, creates a new DeviceGroup actor, otherwise forwards the request to the existing actor..
Acts as a registry of DeviceGroup actors
The only actor available to clients initially.

# DeviceGroup
Manage DeviceActors for a single group (home / building)
Isolates failures to a single group, limiting any impact of a DeviceActor failure to a single group.
Provides a limited context easily queryable for data related to only a single group.
Eliminates the need to filter data to a single group.
Having separate actors for each group supports concurrent processing across groups.
Given a request having certain deviceId, will create a new DeviceActor if none already exists, otherwise forwards the request to existing device actor.

# DeviceActor
Manage all communication with Device sensor's themselves.
For example, they accept and record temperature readings.
Responds to all sensor requests with an acknowledgement.
Responding to the sensor directly provides the sensor with a reference to a device actor with which it can communicate directly, minimizing network traffic and simplifying the message protocol.


Concerns
--------
# Message delivery
Certain guarantees are more expensive than others.

The least expensive guarantee is at most one delivery.  Requires no state.

The most expensive is once and only once.  Requires state at both the sending side and receiving side.

Some delivery guarantees must be performed by the application using various Akka features.

For example, to implement once and only once guarantees, the sender must ensure the client not only received the message, but that it also processed the message completely.  The only way to do this is for the sender to include a correlationId of some kind in the outbound message and for the receiver to respond w/ a reply message containing that correlationId once it has completely processed the message.  

# Protocols
Distributed API's are built around messages as opposed to interfaces.

Invokation of an API is through messages, not method invocation.


Actor Behavior
--------------
A function that defines what an actor does on receipt of a message.

Can be defined statically or dynamically.

See context.become(newBehavior).



Reference
---------
https://doc.akka.io/docs/akka/current/guide/index.html
