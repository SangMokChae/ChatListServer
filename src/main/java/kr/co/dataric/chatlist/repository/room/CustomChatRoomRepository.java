package kr.co.dataric.chatlist.repository.room;

import org.springframework.data.redis.core.ReactiveStreamOperations;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface CustomChatRoomRepository {
	Mono<Long> countUnreadMessages(String roomId, String lastMsgId, LocalDateTime lastReadTime);
}
