package com.hrsphere.common.event;

import java.time.Instant;

public record EventEnvelope<T>(
    String eventId,
    String eventType,
    Instant timestamp,
    String source,
    T payload,
    String version) {}
