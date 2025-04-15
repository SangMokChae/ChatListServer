package kr.co.dataric.chatlist.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.dataric.chatlist.service.chatList.ChatRoomListService;
import kr.co.dataric.chatlist.sink.UserSinkManager;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.dto.ReadCountMessage;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatListWebSocketHandler implements WebSocketHandler {
	
	private final ReactiveStringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final UserSinkManager userSinkManager;
	private final ChatRoomListService chatRoomListService;
	private final JwtProvider jwtProvider;
	// 사용자별 Sink 보관
	private static final ConcurrentHashMap<String, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();
	
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
		
		log.info("🔌 WebSocket 연결됨 - userId: {}", userId);
		Sinks.Many<ChatRoomRedisDto> sink = Sinks.many().multicast().onBackpressureBuffer();
		userSinkManager.register(userId);
		
		chatRoomListService.findAllByParticipant(userId)
			.doOnNext(sink::tryEmitNext)
			.subscribe();
		
		Flux<WebSocketMessage> output = sink.asFlux()
			.map(this::toJson)
			.map(session::textMessage);
		
		Mono<Void> onClose = session.receive().then()
			.doFinally(signal -> {
				log.info("🔌 WebSocket 종료 - userId : {}", userId);
			});
		
		return session.send(output).and(onClose);
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
						log.info("📤 WebSocket 전송 - userId: {}, roomId: {}", userId, dto.getRoomId());
						sink.tryEmitNext(dto);
					});
				}
				return Mono.empty();
			}).subscribe();
	}
}
