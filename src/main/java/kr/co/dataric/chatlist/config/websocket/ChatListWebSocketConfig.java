package kr.co.dataric.chatlist.config.websocket;

import kr.co.dataric.chatlist.handler.ChatListWebSocketHandler;
import kr.co.dataric.common.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ChatListWebSocketConfig {
	
	@Bean
	public Sinks.Many<ChatRoom> chatListSink() {
		return Sinks.many().multicast().onBackpressureBuffer();
	}
	
	@Bean
	public HandlerMapping chatListWebSocketMapping(ChatListWebSocketHandler handler) {
		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/ws/chatList/**", handler);
		
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(1);
		mapping.setUrlMap(map);
		return mapping;
	}
	
	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}
}