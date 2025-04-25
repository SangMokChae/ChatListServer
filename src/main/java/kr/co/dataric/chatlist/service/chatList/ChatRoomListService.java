package kr.co.dataric.chatlist.service.chatList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.dto.friends.FriendsChatRoomRequest;
import kr.co.dataric.chatlist.repository.room.ChatRoomRepository;
import kr.co.dataric.chatlist.repository.room.CustomChatRoomRepository;
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
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomListService {
	
	private final ChatRoomRepository chatRoomRepository;
	private final ReactiveStringRedisTemplate redisTemplate;
	private final CustomChatRoomRepository customChatRoomRepository;
	private final ObjectMapper objectMapper;
	private final RoomIdGenerator roomIdGenerator;
	
	/**
	 * ✅ userId가 포함된 모든 채팅방 반환 (Mongo 기준 → Redis 캐싱 포함)
	 */
	public Flux<ChatRoomRedisDto> findAllByParticipant(String userId) {
		return chatRoomRepository.findByParticipantsContaining(userId)
			.flatMap(entity -> {
				ChatRoomRedisDto dto = ChatRoomRedisDto.builder()
					.roomId(entity.getRoomId())
					.roomName(entity.getRoomName())
					.lastMessage(entity.getLastMessage())
					.lastMessageTime(entity.getLastMessageTime())
					.participants(entity.getParticipants())
					.build();
				
				String roomId = dto.getRoomId();
				List<String> participants = dto.getParticipants();
				
				// ✅ 각 유저별 last_read 기반 unread count 계산
				Flux<Map.Entry<String, Long>> unreadCountsFlux = Flux.fromIterable(participants)
					.flatMap(uid -> {
						String key = "last_read:" + roomId + ":" + uid;
						return redisTemplate.opsForValue().get(key)
							.flatMap(lastRead -> {
								String[] parts = lastRead.split("_");
								if (parts.length < 2) return Mono.just(Map.entry(uid, 1L));
								String lastMsgId = parts[0];
								LocalDateTime lastReadTime = LocalDateTime.parse(parts[1]);
								return customChatRoomRepository.countUnreadMessages(roomId, lastMsgId, lastReadTime)
									.map(cnt -> Map.entry(uid, cnt));
							})
							.switchIfEmpty(Mono.just(Map.entry(uid, 1L))); // 읽은 기록이 없으면 기본값 1
					});
				
				// ✅ readCountMap 세팅 후 Redis 저장
				return unreadCountsFlux.collectMap(Map.Entry::getKey, Map.Entry::getValue)
					.flatMap(readCountMap -> {
						dto.setReadCountMap(readCountMap);
						String redisKey = "chat_room:" + roomId;
						String redisValue;
						try {
							redisValue = objectMapper.writeValueAsString(dto);
						} catch (JsonProcessingException e) {
							log.error("Redis 직렬화 실패 - roomId: {}", roomId, e);
							return Mono.just(dto); // Redis 저장 실패해도 dto는 리턴
						}
						
						return redisTemplate.opsForValue().set(redisKey, redisValue, Duration.ofDays(30))
							.doOnSuccess(ok -> log.info("🔁 Redis 저장 완료 - {}", redisKey))
							.thenReturn(dto);
					});
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