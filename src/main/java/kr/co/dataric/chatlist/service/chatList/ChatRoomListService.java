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

import java.time.Duration;
import java.time.LocalDateTime;
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
	
	/**
	 * ✅ userId가 포함된 모든 채팅방 반환 (Mongo 기준 → Redis 캐싱 포함)
	 */
	public Flux<ChatRoomRedisDto> findAllByParticipant(String userId) {
		return chatRoomRepository.findByParticipantsContaining(userId)
			.map(entity -> ChatRoomRedisDto.builder()
				.roomId(entity.getRoomId())
				.roomName(entity.getRoomName())
				.lastMessage(entity.getLastMessage())
				.lastMessageTime(entity.getLastMessageTime())
				.userIds(entity.getParticipants())
				.build())
			.flatMap(dto -> {
				// Redis 저장
				String redisKey = "chat_room:" + dto.getRoomId();
				String redisValue;
				try {
					redisValue = objectMapper.writeValueAsString(dto);
				} catch (Exception e) {
					log.error("❌ Redis 직렬화 실패 - roomId: {}", dto.getRoomId(), e);
					return Mono.empty();
				}
				
				return redisTemplate.opsForValue().set(redisKey, redisValue, Duration.ofDays(30))
					.thenReturn(dto);
			});
	}
	
	/**
	 * ✅ 특정 채팅방의 참여자 목록 조회
	 */
	public Flux<String> findAllParticipantsByRoomId(String roomId) {
		return chatRoomRepository.findByRoomId(roomId)
			.flatMapMany(room -> Flux.fromIterable(room.getParticipants()));
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
	
}