package kr.co.dataric.chatlist.controller.chatList;

import kr.co.dataric.chatlist.service.chatList.ChatRoomListService;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatList")
public class ChatListController {
	
	private final JwtProvider jwtProvider;
	private final ChatRoomListService chatRoomListService;
	
	/**
	 * ✅ 로그인한 사용자 참여 채팅방 목록 조회 (Redis DTO 기반)
	 */
//	@GetMapping
//	public Flux<ChatRoomRedisDto> getChatList(ServerHttpRequest request) {
//		return Mono.justOrEmpty(request.getCookies().getFirst("accessToken"))
//			.map(HttpCookie::getValue)
//			.map(jwtProvider::extractUserId)
//			.flatMapMany(chatRoomListService::findAllByParticipant)
//			.doOnNext(room -> log.info("채팅방 반환 - roomId: {}", room.getRoomId()));
//	}

	/**
	 * ✅ JWT 인증 기반 채팅방 목록 조회 (Redis DTO 기반)
	 */
	@PostMapping("/list")
	public Flux<ChatRoomRedisDto> getMyChatRooms(ServerWebExchange exchange) {
		String userId = extractUserIdFromCookies(exchange);
		
		if (userId == null) {
			log.warn("❌ getMyChatRooms - userId 추출 실패 (Access & RefreshToken 모두 없음)");
			return Flux.empty();
		}
		
		log.info("✅ getMyChatRooms - userId: {}", userId);
		return chatRoomListService.findAllByParticipant(userId);
	}
	
	private String extractUserIdFromCookies(ServerWebExchange exchange) {
		HttpCookie accessTokenCookie = exchange.getRequest().getCookies().getFirst("accessToken");
		HttpCookie refreshTokenCookie = exchange.getRequest().getCookies().getFirst("refreshToken");
		
		return Optional.ofNullable(accessTokenCookie)
			.map(cookie -> jwtProvider.extractUserId(cookie.getValue()))
			.or(() -> Optional.ofNullable(refreshTokenCookie)
				.map(cookie -> jwtProvider.extractUserIdIgnoreExpiration(cookie.getValue())))
			.orElse(null);
	}

}

