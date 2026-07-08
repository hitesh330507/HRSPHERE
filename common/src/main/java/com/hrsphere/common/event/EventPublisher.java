package com.hrsphere.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Best-effort Redis Pub/Sub publisher for at-most-once domain events. Publishing failures are
 * logged and swallowed so a service's primary operation, such as user registration, is not rolled
 * back by transient event infrastructure issues.
 */
@Component
public class EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
  private static final String EVENT_VERSION = "v1";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public EventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public <T> void publish(String eventType, String source, T payload) {
    EventEnvelope<T> envelope =
        new EventEnvelope<>(
            UUID.randomUUID().toString(), eventType, Instant.now(), source, payload, EVENT_VERSION);
    try {
      String json = objectMapper.writeValueAsString(envelope);
      redisTemplate.convertAndSend(eventType, json);
      log.info(
          "Published event: type={}, eventId={}, source={}", eventType, envelope.eventId(), source);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event type={}: {}", eventType, e.getMessage(), e);
    } catch (RuntimeException e) {
      log.error("Failed to publish event type={}: {}", eventType, e.getMessage(), e);
    }
  }
}
