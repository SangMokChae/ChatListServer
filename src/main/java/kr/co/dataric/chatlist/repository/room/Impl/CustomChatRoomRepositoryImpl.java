package kr.co.dataric.chatlist.repository.room.Impl;

import kr.co.dataric.chatlist.repository.room.CustomChatRoomRepository;
import kr.co.dataric.common.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomChatRoomRepositoryImpl implements CustomChatRoomRepository {
	
	private final ReactiveMongoTemplate mongoTemplate;
	
	@Override
	public Mono<Long> countUnreadMessages(String roomId, String lastMsgId, LocalDateTime lastReadTime) {
		LocalDateTime lastTime = lastReadTime.truncatedTo(ChronoUnit.MILLIS);
		
		Query query = new Query(new Criteria().andOperator(
			Criteria.where("roomId").is(roomId),
			Criteria.where("timestamp").gte(lastReadTime),
			Criteria.where("msgId").ne(lastMsgId)
		));
		return mongoTemplate.count(query, ChatMessage.class);
	}
}
