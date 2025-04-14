package kr.co.dataric.chatlist.service.chatList;

import kr.co.dataric.chatlist.dto.friends.FriendsChatRoomRequest;
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
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomListService {
	
	private final ChatRoomRepository chatRoomRepository;
	private final ReactiveStringRedisTemplate redisTemplate;
	private final RoomIdGenerator roomIdGenerator;
	
	public Flux<ChatRoomRedisDto> findAllByParticipant(String userId) {
		return chatRoomRepository.findByParticipantsContaining(userId)
			.doOnNext(result -> log.info("resl :: {}", result))
			.flatMap(room -> {
				String key = "chat_room:" + room.getRoomId();
				return redisTemplate.opsForValue().get(key)
					.flatMap(value -> {
						if (value != null && value.contains("||")) {
							String[] parts = value.split("\\|\\|");
							ChatRoomRedisDto dto = new ChatRoomRedisDto();
							dto.setRoomId(room.getRoomId());
							dto.setRoomName(room.getRoomName());
							dto.setUserIds(room.getParticipants());
							dto.setLastMessage(parts[0]);
							dto.setLastMessageTime(LocalDateTime.parse(parts[1]));
							return Mono.just(dto);
						} else {
							return Mono.just(ChatRoomRedisDto.from(room));
						}
					})
					.switchIfEmpty(Mono.just(ChatRoomRedisDto.from(room)));
			})
			.sort(Comparator.comparing(ChatRoomRedisDto::getLastMessageTime).reversed());
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
