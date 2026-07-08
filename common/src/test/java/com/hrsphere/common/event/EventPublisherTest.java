package com.hrsphere.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

class EventPublisherTest {

  @Test
  void publish_shouldSendSerializedEnvelopeToMatchingChannel() throws Exception {
    StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    EventPublisher publisher = new EventPublisher(redisTemplate, objectMapper);
    TestPayload payload =
        new TestPayload("hitesh", "hitesh@hrsphere.dev", List.of("ROLE_EMPLOYEE"));

    publisher.publish(EventType.USER_CREATED, "auth-service", payload);

    ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(redisTemplate).convertAndSend(eq(EventType.USER_CREATED), jsonCaptor.capture());
    JsonNode json = objectMapper.readTree(jsonCaptor.getValue());
    assertThat(json.get("eventType").asText()).isEqualTo(EventType.USER_CREATED);
    assertThat(json.get("source").asText()).isEqualTo("auth-service");
    assertThat(json.get("version").asText()).isEqualTo("v1");
    assertThat(json.get("payload").get("username").asText()).isEqualTo("hitesh");
  }

  @Test
  void publish_shouldNotThrowWhenSerializationFails() throws Exception {
    StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
    EventPublisher publisher = new EventPublisher(redisTemplate, objectMapper);
    given(objectMapper.writeValueAsString(any()))
        .willThrow(
            new JsonProcessingException("boom") {
              private static final long serialVersionUID = 1L;
            });

    assertThatCode(() -> publisher.publish(EventType.USER_CREATED, "auth-service", new Object()))
        .doesNotThrowAnyException();
  }

  record TestPayload(String username, String email, List<String> roles) {}
}
