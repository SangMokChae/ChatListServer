package kr.co.dataric.chatlist.config.redis;

import kr.co.dataric.chatlist.subscriber.ChatListRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {
	
	private final ChatListRedisSubscriber chatListRedisSubscriber;
	
	@Bean
	public RedisMessageListenerContainer container(RedisConnectionFactory factory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.addMessageListener(listenerAdapter(), topic()); // 등록 되어야 함
		return container;
	}
	
	@Bean
	public ChannelTopic topic() {
		return new ChannelTopic("chatListUpdate"); // 사용 중인 Redis Pub/Sub 채널명
	}
	
	@Bean
	public MessageListenerAdapter listenerAdapter() {
		return new MessageListenerAdapter(chatListRedisSubscriber, "onMessage");
	}
	
	
}
