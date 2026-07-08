# HRSphere - Event Catalog

| Event Type | Channel | Publisher | Subscriber(s) | Payload | Added |
|---|---|---|---|---|---|
| user.created | user.created | auth-service | employee-service | UserCreatedPayload (username, email, roles) | Day 14 |

## Payload schemas

### UserCreatedPayload

- username: String
- email: String
- roles: List<String>

## Conventions

- Envelope: see `EventEnvelope<T>` in the common module
- Channel naming: matches `eventType` exactly
- Delivery: Redis Pub/Sub is fire-and-forget -- no persistence, no replay, no guaranteed delivery. Consumers MUST be idempotent and MUST NOT assume every event will be received.
- Every new event added to the system gets a row here in the same commit that implements it, using the same discipline as `databases.sql` and `gateway-routes.md`.
- Event publishing is best-effort and at-most-once. A publish failure is logged but must not roll back the caller's primary operation.

## How to subscribe to an event

1. Create a payload record/class matching the event's schema.
2. Create a concrete subscriber extending `AbstractEventSubscriber<YourPayloadType>`.
3. Implement `handleEvent()`, `getPayloadType()`, and `getConsumerName()`.
4. Register a `RedisMessageListenerContainer` bean in a service-local `@Configuration` class, mapping the subscriber to the event channel with `container.addMessageListener(subscriber, new ChannelTopic(eventType))`.
5. Add a row to this event catalog.
