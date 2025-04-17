package kr.co.dataric.chatlist.repository.room;

import kr.co.dataric.common.entity.ChatRoom;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatRoomRepository extends ReactiveMongoRepository<ChatRoom, String> {
	Flux<ChatRoom> findByParticipantsContaining(String userId);
	
	@Query("{ 'participants': { $all: [?0, ?1] }, 'participants.2': { $exists: false } }")
	Mono<ChatRoom> findOneToOneRoom(String userId, String friendId);
	
	/**
	 * 채팅방 ID로 단일 채팅방 조회
	 */
	Mono<ChatRoom> findByRoomId(String roomId);
}

