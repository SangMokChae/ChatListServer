package kr.co.dataric.chatlist.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import kr.co.dataric.chatlist.handler.ChatListWebSocketHandler;
import kr.co.dataric.chatlist.sink.UserSinkManager;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;


// ChatRoomRedisSubscriber.java - Redis 메시지 수신 후 WebSocket 전송 전에 캐시값 반영 가능
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomRedisSubscriber {
	
	private final ReactiveStringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final ChatListWebSocketHandler chatListWebSocketHandler;
	private final UserSinkManager userSinkManager;
	
	@PostConstruct
	public void subscribe() {
		redisTemplate.listenToChannel("chat-room-update")
			.publishOn(Schedulers.boundedElastic())
			.subscribe(message -> {
				try {
					ChatRoomRedisDto dto = objectMapper.readValue(message.getMessage(), ChatRoomRedisDto.class);
					log.info("📨 Redis PubSub 수신 - roomId : {}, lastMsg: {}", dto.getRoomId(), dto.getLastMessage());
					chatListWebSocketHandler.emitToRoom(dto.getRoomId(), dto);
				} catch (Exception e) {
					log.error("❌ Redis PubSub 처리 실패", e);
				}
			});
	}
}