package com.hrsphere.employee.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrsphere.common.event.AbstractEventSubscriber;
import com.hrsphere.common.event.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedEventSubscriber extends AbstractEventSubscriber<UserCreatedPayload> {

  private static final Logger log = LoggerFactory.getLogger(UserCreatedEventSubscriber.class);
  private static final String CONSUMER_NAME = "employee-service";

  public UserCreatedEventSubscriber(ObjectMapper objectMapper, StringRedisTemplate redisTemplate) {
    super(objectMapper, redisTemplate);
  }

  @Override
  protected void handleEvent(EventEnvelope<UserCreatedPayload> envelope) {
    UserCreatedPayload payload = envelope.payload();
    log.info(
        "Received user.created event: username={}, email={}, roles={}, eventId={}, publishedAt={}",
        payload.username(),
        payload.email(),
        payload.roles(),
        envelope.eventId(),
        envelope.timestamp());
    // Option A for Day 14: log only. Employee creation remains an explicit HR workflow.
  }

  @Override
  protected Class<UserCreatedPayload> getPayloadType() {
    return UserCreatedPayload.class;
  }

  @Override
  protected String getConsumerName() {
    return CONSUMER_NAME;
  }
}
