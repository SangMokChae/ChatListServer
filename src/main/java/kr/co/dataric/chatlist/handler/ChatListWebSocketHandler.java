package kr.co.dataric.chatlist.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.service.chatList.ChatRoomListService;
import kr.co.dataric.chatlist.sink.UserSinkManager;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatListWebSocketHandler implements WebSocketHandler {
	
	private final ReactiveStringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final UserSinkManager userSinkManager;
	private final ChatRoomListService chatRoomListService;
	private final JwtProvider jwtProvider;
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String token = Optional.ofNullable(session.getHandshakeInfo().getCookies().getFirst("accessToken"))
			.map(HttpCookie::getValue)
			.orElse(null);
		
		String userId = jwtProvider.extractUserId(token);
		if (userId == null) {
			log.warn("WebSocket 연결 실패 - 유효한 토큰 없음");
			return session.close();
		}
		
		log.info("WebSocket 연결됨 - userId : {}", userId);
		Sinks.Many<ChatRoomRedisDto> sink = Sinks.many().multicast().onBackpressureBuffer();
		userSinkManager.register(userId, sink);
		
		// ✅ WebSocket 종료 처리
		Mono<Void> onClose = session.receive().then()
			.doFinally(signal -> {
				log.info("🔌 WebSocket 종료 - userId : {}", userId);
				userSinkManager.remove(userId, sink);
			});
		
		// ✅ Mongo 기준 최초 조회
		Mono<Void> mongoInit = chatRoomListService.findAllByParticipant(userId)
			.doOnNext(sink::tryEmitNext)
			.then();
		
		// 2. Redis 기준 Scan 방식으로 실시간 방 목록 가져오기
		Mono<Void> redisInit = redisTemplate
			.getConnectionFactory()
			.getReactiveConnection()
			.keyCommands()
			.scan(ScanOptions.scanOptions().match("chat_room:*").count(100).build())
			.map(keyBuffer -> keyBuffer.toString())
			.flatMap(roomKey -> redisTemplate.opsForValue().get(roomKey)
				.map(raw -> {
					try {
						String[] parts = raw.split("\\|\\|");
						if (parts.length != 2) {
							log.warn("⚠️ Redis 포맷 이상 - key: {}", roomKey);
							return null;
						}
						return ChatRoomRedisDto.builder()
							.roomId(extractRoomIdFromKey(roomKey))
							.lastMessage(parts[0])
							.lastMessageTime(LocalDateTime.parse(parts[1]))
							.build();
					} catch (Exception e) {
						log.warn("❌ Redis 파싱 실패 - key: {}, 이유: {}", roomKey, e.getMessage());
						return null;
					}
				}))
			.filter(Objects::nonNull)
			.sort((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()))
			.doOnNext(sink::tryEmitNext)
			.then();
		
		// ✅ WebSocket 메시지 스트림 전송
		Flux<WebSocketMessage> output = sink.asFlux()
			.map(this::toJson)
			.map(session::textMessage);
		
		// ✅ 병렬 실행 (Mongo + Redis)
		return Mono.when(mongoInit, redisInit)
			.then(session.send(output).and(onClose));
	}
	
	private String toJson(ChatRoomRedisDto dto) {
		try {
			return objectMapper.writeValueAsString(dto);
		} catch (Exception e) {
			log.error("❌ 직렬화 실패", e);
			return "{}";
		}
	}
	
	public void emitToRoom(String roomId, ChatRoomRedisDto dto) {
		chatRoomListService.findAllParticipantsByRoomId(roomId)
			.flatMap(userId -> {
				Set<Sinks.Many<ChatRoomRedisDto>> sinks = userSinkManager.get(userId);
				if (sinks != null) {
					sinks.forEach(sink -> {
						log.info("📤 WebSocket 전송 - userId: {}, roomId: {}", userId, roomId);
						sink.tryEmitNext(dto);
					});
				}
				return Mono.empty();
			}).subscribe();
	}
	
	private String extractRoomIdFromKey(String key) {
		if (key.startsWith("chat_room:")) {
			return key.substring("chat_room:".length());
		}
		return key;
	}

}