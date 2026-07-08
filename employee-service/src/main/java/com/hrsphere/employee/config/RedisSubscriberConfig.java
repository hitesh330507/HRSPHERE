package com.hrsphere.employee.config;

import com.hrsphere.common.event.EventType;
import com.hrsphere.employee.event.UserCreatedEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisSubscriberConfig {

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory, UserCreatedEventSubscriber userCreatedSubscriber) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(userCreatedSubscriber, new ChannelTopic(EventType.USER_CREATED));
    return container;
  }
}
