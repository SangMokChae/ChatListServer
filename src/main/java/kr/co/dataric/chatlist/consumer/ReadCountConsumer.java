package kr.co.dataric.chatlist.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.dto.read.ReadCountMessage;
import kr.co.dataric.chatlist.handler.ChatListWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadCountConsumer {
	
	private final ObjectMapper objectMapper;
	private final ChatListWebSocketHandler webSocketHandler;
	
	@KafkaListener(topics = "chat.read.count", groupId = "chat-listener")
	public void onReadCount(String message) throws JsonProcessingException {
		ReadCountMessage msg = objectMapper.readValue(message, ReadCountMessage.class);
		webSocketHandler.emitToRoom(msg.getRoomId(), msg);
	}
	
}
