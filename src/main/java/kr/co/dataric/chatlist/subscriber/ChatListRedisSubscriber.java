package kr.co.dataric.chatlist.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.handler.ChatListWebSocketHandler;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.dto.ReadCountMessage;
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
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		String body = new String(message.getBody(), StandardCharsets.UTF_8);
		log.info("Redis 수신 - channel: {}, body: {}", channel, body);
		
		try {
			if ("chatListUpdate".equals(channel)) {
				String json = new String(message.getBody(), StandardCharsets.UTF_8);
				ChatRoomRedisDto dto = objectMapper.readValue(json, ChatRoomRedisDto.class);
				handler.emitToRoom(dto.getRoomId(), dto);
			} else if ("chatReadUpdate".equals(channel)) {
				// TODO: ReadCountMessage 또는 유사 DTO로 처리
				// 예: roomId, readCount 포함
				ReadCountMessage readCount = objectMapper.readValue(body, ReadCountMessage.class);
				handler.emitReadCount(readCount.getRoomId(), readCount.getReadCount());
			} else {
				log.warn("알 수 없는 Redis 채널 수신: {}", channel);
			}
		} catch (Exception e) {
			log.error("Redis 수신 처리 실패", e);
		}
	}
}
