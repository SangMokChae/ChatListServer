package kr.co.dataric.chatlist.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.handler.ChatListWebSocketHandler;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.dto.ReadCountMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Slf4j
@Component
@RequiredArgsConstructor
public class ReadCountConsumer {
	
	private final ObjectMapper objectMapper;
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	private final ChatListWebSocketHandler chatListWebSocketHandler;
	
	@KafkaListener(topics = "chat.read.count", groupId = "chat-room-group")
	public void onReadCount(ReadCountMessage msg) {
		log.info("📥 Kafka 읽음 수 수신: {}", msg);
		
		// Null 체크 등 방어 로직
		if (msg == null || msg.getRoomId() == null) {
			log.warn("⚠️ 유효하지 않은 ReadCountMessage 수신 - 무시됨: {}", msg);
			return;
		}
		
		// Redis에 저장된 채팅방 DTO 읽기 (필수)
		String redisKey = String.format("caht_room:%s:%s:%d",
			msg.getRoomId(), getLastSenderName(msg), msg.getReadCount());
		
		redisTemplate.opsForValue()
			.get(redisKey)
			.flatMap(json -> {
				try {
					ChatRoomRedisDto original = objectMapper.readValue(json, ChatRoomRedisDto.class);
					ChatRoomRedisDto updated = ChatRoomRedisDto.builder()
						.roomId(original.getRoomId())
						.roomName(original.getRoomName())
						.lastMessage(original.getLastMessage())
						.lastMessageTime(original.getLastMessageTime())
						.readCount(msg.getReadCount()) // dlfrdma tn rudtls
						.build();
					
					// WebSocket Sink전파
					chatListWebSocketHandler.emitToRoom(msg.getRoomId(), updated);
				} catch (Exception e) {
					log.error("ReadCountMessage 처리 중 역직렬화 실패", e);
				}
				return Mono.empty();
			})
			.subscribe();
	}
	
	private String getLastSenderName(ReadCountMessage msg) {
		if (msg.getSender() != null && !msg.getSender().isBlank()) {
			return msg.getSender();
		}
		// fallback - sender 없을 경우 기본값 지정
		return "unknown";
	}
	
}
