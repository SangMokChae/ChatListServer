package kr.co.dataric.chatlist.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.handler.ChatListWebSocketHandler;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatListRedisSubscriber implements MessageListener {
	
	private final ObjectMapper objectMapper;
	private final ChatListWebSocketHandler handler;
	
	@Override
	public void onMessage(Message message, byte[] pattern) {
		log.info("msg :: {}", message);
		
		try {
			String json = new String(message.getBody(), StandardCharsets.UTF_8);
			ChatRoomRedisDto dto = objectMapper.readValue(json, ChatRoomRedisDto.class);
			handler.emitToRoom(dto.getRoomId(), dto);
		} catch (Exception e) {
			log.error("Redis 수신 처리 실패", e);
		}
	}
}
