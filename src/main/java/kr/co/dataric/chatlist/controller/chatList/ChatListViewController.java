package kr.co.dataric.chatlist.controller.chatList;

import kr.co.dataric.chatlist.utils.extract.ExtractToken;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/view")
@RequiredArgsConstructor
public class ChatListViewController {
	
	private final ExtractToken extractToken;
	
	@GetMapping("/chatListView")
	public Mono<Rendering> chatListView(ServerHttpRequest request) {
		return extractToken.extractUserIdFromCookies(request)
			.map(userId -> {
				log.info("✅ ChatListView - userId: {}", userId);
				return Rendering.view("/chatList/chatListView")
					.modelAttribute("userId", userId)
					.build();
			})
			.switchIfEmpty(Mono.defer(() -> {
				log.warn("❌ accessToken & refreshToken 모두 없음 또는 검증 실패 - redirect to logout");
				return Mono.just(Rendering.redirectTo("/logout").build());
			}));
	}
	
}
