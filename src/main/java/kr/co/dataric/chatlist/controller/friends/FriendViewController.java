package kr.co.dataric.chatlist.controller.friends;

import kr.co.dataric.chatlist.service.friend.FriendService;
import kr.co.dataric.chatlist.utils.extract.ExtractToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FriendViewController {
	
	private final ExtractToken extractToken;
	private final FriendService friendService;
	
	@GetMapping("/view/friendListView")
	public Mono<Rendering> friendListView(ServerHttpRequest request) {
		return extractToken.extractUserIdFromCookies(request)
			.flatMap(userId -> friendService.getFriendList(userId)
				.collectList()
				.map(friends -> Rendering.view("/friend/friendListView")
					.modelAttribute("friendList", friends)
					.modelAttribute("userId", userId)
					.build())
			)
			.switchIfEmpty(Mono.just(Rendering.redirectTo("/logout").build()));
	}
	
}
