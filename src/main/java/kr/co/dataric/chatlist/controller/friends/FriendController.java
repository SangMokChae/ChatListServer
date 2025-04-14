package kr.co.dataric.chatlist.controller.friends;

import kr.co.dataric.chatlist.dto.friends.FriendsChatRoomRequest;
import kr.co.dataric.chatlist.service.chatList.ChatRoomListService;
import kr.co.dataric.chatlist.service.friend.FriendService;
import kr.co.dataric.common.entity.Friends;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
	
	private final FriendService friendService;
	private final ChatRoomListService chatRoomListService;
	private final JwtProvider jwtProvider;
	
	// userId의 보안적으로 /list는 get -> post로 변경
	@PostMapping("/list")
	public Flux<Friends> getFriendList(@RequestParam String userId) {
		return friendService.getFriendList(userId);
	}
	
	@PostMapping("/chatRoom")
	public Mono<ResponseEntity<Map<String, String>>> createOrGetChatRoom(
		@RequestBody FriendsChatRoomRequest friendDto,
		ServerWebExchange exchange) {
		String userId = extractUserIdFromCookies(exchange);
		
		friendDto.setUserId(userId);
		
		return chatRoomListService.createOrGetOneToOneRoom(friendDto)
			.map(chatRoom -> Map.of("roomId", chatRoom.getRoomId()))
			.map(ResponseEntity::ok);
	}
	
	private String extractUserIdFromCookies(ServerWebExchange exchange) {
		HttpCookie accessTokenCookie = exchange.getRequest().getCookies().getFirst("accessToken");
		HttpCookie refreshTokenCookie = exchange.getRequest().getCookies().getFirst("refreshToken");
		
		String user = jwtProvider.extractUserIdIgnoreExpiration(Objects.requireNonNull(accessTokenCookie).getValue());
		
		if (user == null) {
			user = jwtProvider.extractUserIdIgnoreExpiration(Objects.requireNonNull(refreshTokenCookie).getValue());
		}
		
		return user;
	}
	
}
