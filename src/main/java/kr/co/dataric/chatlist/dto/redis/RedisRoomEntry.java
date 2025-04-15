package kr.co.dataric.chatlist.dto.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.common.dto.ChatRoomRedisDto;

import java.time.LocalDateTime;

public record RedisRoomEntry(String key, String value) {
	public LocalDateTime timeStamp() {
		// Redis에 저장된 ChatRoomRedisDto -> 마지막 메시지 시간 기준 정렬
		try {
			ChatRoomRedisDto dto = new ObjectMapper().readValue(value, ChatRoomRedisDto.class);
			return dto.getLastMessageTime();
		} catch (Exception e) {
			return LocalDateTime.now();
		}
	}
}
