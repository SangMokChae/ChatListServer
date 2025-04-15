package kr.co.dataric.chatlist.service.chatList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.dto.friends.FriendsChatRoomRequest;
import kr.co.dataric.chatlist.dto.redis.RedisRoomEntry;
import kr.co.dataric.chatlist.repository.room.ChatRoomRepository;
import kr.co.dataric.chatlist.utils.generator.RoomIdGenerator;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomListService {
	
	private final ChatRoomRepository chatRoomRepository;
	private final ReactiveStringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final RoomIdGenerator roomIdGenerator;
	
	public Flux<ChatRoomRedisDto> findAllByParticipant(String userId) {
		String redisKeySet = "chat_room_keys:" + userId;
		
		return redisTemplate.opsForSet().members(redisKeySet)
			.flatMap(roomKey ->
				redisTemplate.opsForValue().get(roomKey)
					.map(json -> {
						try {
							ChatRoomRedisDto dto = objectMapper.readValue(json, ChatRoomRedisDto.class);
							return dto;
						} catch (Exception e) {
							log.warn("❌ Redis JSON 역직렬화 실패 - key: {}", roomKey, e.getMessage());
							return null;
						}
					})
			)
			.filter(Objects::nonNull)
			.sort((a, b) -> {
				try {
					return b.getLastMessageTime().compareTo(a.getLastMessageTime());
				} catch (Exception e) {
					log.warn("⚠️ 정렬 실패 - fallback 적용");
					return 0;
				}
			}); // 최신 메시지 순
	}
	
	public Mono<ChatRoom> createOrGetOneToOneRoom(FriendsChatRoomRequest friendDto) {
		String userId = friendDto.getUserId();
		String friendId = friendDto.getFriendId();
		List<String> userList = List.of(userId, friendId);
		
		return chatRoomRepository.findOneToOneRoom(userId, friendId)
			.switchIfEmpty(Mono.defer(() -> {
				ChatRoom newRoom = ChatRoom.builder()
					.roomId(roomIdGenerator.generateRoomId(userList))
					.participants(userList)
					.createAt(LocalDateTime.now())
					.roomName(friendId)
					.build();
				return Mono.just(newRoom); // DB에는 아직 저장하지 않음
			}));
	}
	
	public Flux<String> findAllParticipantsByRoomId(String roomId) {
		String key = "chatRoom:participants:"+roomId;
		return redisTemplate.opsForSet().members(key)
			.doOnNext(userId -> log.debug("참여자 조회 - roomId: {}, userI: {}", roomId, userId));
	}
}
