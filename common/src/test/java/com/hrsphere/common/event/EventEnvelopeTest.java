package com.hrsphere.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void shouldRoundTripGenericEnvelope() throws Exception {
    TestPayload payload =
        new TestPayload("hitesh", "hitesh@hrsphere.dev", List.of("ROLE_EMPLOYEE"));
    EventEnvelope<TestPayload> envelope =
        new EventEnvelope<>(
            "event-1",
            EventType.USER_CREATED,
            Instant.parse("2026-07-08T10:15:30Z"),
            "test",
            payload,
            "v1");

    String json = objectMapper.writeValueAsString(envelope);
    JavaType type =
        objectMapper
            .getTypeFactory()
            .constructParametricType(EventEnvelope.class, TestPayload.class);
    EventEnvelope<TestPayload> restored = objectMapper.readValue(json, type);

    assertThat(restored).isEqualTo(envelope);
  }

  record TestPayload(String username, String email, List<String> roles) {}
}
