package com.hrsphere.employee.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrsphere.common.event.EventEnvelope;
import com.hrsphere.common.event.EventType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UserCreatedEventSubscriberTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void handleEvent_shouldCompleteWithoutMutation() {
    UserCreatedEventSubscriber subscriber =
        new UserCreatedEventSubscriber(
            objectMapper, org.mockito.Mockito.mock(StringRedisTemplate.class));
    EventEnvelope<UserCreatedPayload> envelope = envelope("event-1");

    assertThatCode(() -> subscriber.handleEvent(envelope)).doesNotThrowAnyException();
  }

  @Test
  void onMessage_shouldSkipDuplicateEventIds() throws Exception {
    StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOperations =
        org.mockito.Mockito.mock(ValueOperations.class);
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(redisTemplate.hasKey("processed_events:employee-service:event-1"))
        .willReturn(false)
        .willReturn(true);
    UserCreatedEventSubscriber subscriber =
        spy(new UserCreatedEventSubscriber(objectMapper, redisTemplate));
    Message message = message(envelope("event-1"));

    subscriber.onMessage(message, null);
    subscriber.onMessage(message, null);

    verify(subscriber, times(1)).handleEvent(any());
    verify(valueOperations)
        .set(eq("processed_events:employee-service:event-1"), eq("1"), eq(Duration.ofHours(24)));
  }

  @Test
  void onMessage_shouldNotThrowForMalformedJson() {
    StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    UserCreatedEventSubscriber subscriber =
        new UserCreatedEventSubscriber(objectMapper, redisTemplate);
    Message message = org.mockito.Mockito.mock(Message.class);
    doReturn("{malformed-json".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        .when(message)
        .getBody();

    assertThatCode(() -> subscriber.onMessage(message, null)).doesNotThrowAnyException();
  }

  private EventEnvelope<UserCreatedPayload> envelope(String eventId) {
    return new EventEnvelope<>(
        eventId,
        EventType.USER_CREATED,
        Instant.parse("2026-07-08T10:15:30Z"),
        "auth-service",
        new UserCreatedPayload("hitesh", "hitesh@hrsphere.dev", List.of("ROLE_EMPLOYEE")),
        "v1");
  }

  private Message message(EventEnvelope<UserCreatedPayload> envelope) throws Exception {
    Message message = org.mockito.Mockito.mock(Message.class);
    doReturn(objectMapper.writeValueAsBytes(envelope)).when(message).getBody();
    return message;
  }
}
