package com.hrsphere.common.event;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class AbstractEventSubscriber<T> implements MessageListener {

  private static final Logger log = LoggerFactory.getLogger(AbstractEventSubscriber.class);
  private static final Duration PROCESSED_EVENT_TTL = Duration.ofHours(24);

  private final ObjectMapper objectMapper;
  private final StringRedisTemplate redisTemplate;

  protected AbstractEventSubscriber(ObjectMapper objectMapper, StringRedisTemplate redisTemplate) {
    this.objectMapper = objectMapper;
    this.redisTemplate = redisTemplate;
  }

  protected abstract Class<T> getPayloadType();

  protected abstract void handleEvent(EventEnvelope<T> envelope);

  protected abstract String getConsumerName();

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      JavaType envelopeType =
          objectMapper
              .getTypeFactory()
              .constructParametricType(EventEnvelope.class, getPayloadType());
      EventEnvelope<T> envelope = objectMapper.readValue(json, envelopeType);

      if (isDuplicate(envelope.eventId())) {
        log.info("Skipping duplicate event: eventId={}", envelope.eventId());
        return;
      }

      handleEvent(envelope);
      markProcessed(envelope.eventId());
    } catch (Exception e) {
      log.error("Failed to process event message: {}", e.getMessage(), e);
    }
  }

  private boolean isDuplicate(String eventId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(processedEventKey(eventId)));
  }

  private void markProcessed(String eventId) {
    // This is a lightweight Pub/Sub duplicate guard. True retry/replay semantics would require
    // Redis Streams or Kafka; Redis Pub/Sub itself does not redeliver missed events.
    redisTemplate.opsForValue().set(processedEventKey(eventId), "1", PROCESSED_EVENT_TTL);
  }

  private String processedEventKey(String eventId) {
    return "processed_events:" + getConsumerName() + ":" + eventId;
  }
}
