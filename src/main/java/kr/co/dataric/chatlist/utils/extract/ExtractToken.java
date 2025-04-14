package kr.co.dataric.chatlist.utils.extract;

import kr.co.dataric.common.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractToken {
	
	private final JwtProvider jwtProvider;
	
	public Mono<String> extractUserIdFromCookies(ServerHttpRequest request) {
		Optional<HttpCookie> accessTokenCookie = Optional.ofNullable(request.getCookies().getFirst("accessToken"));
		Optional<HttpCookie> refreshTokenCookie = Optional.ofNullable(request.getCookies().getFirst("refreshToken"));
		
		// accessToken → refreshToken 순서로 시도
		String accessUserId = extractUserId(accessTokenCookie);
		if (accessUserId != null) return Mono.just(accessUserId);
		
		String refreshUserId = extractUserId(refreshTokenCookie);
		if (refreshUserId != null) return Mono.just(refreshUserId);
		
		return Mono.empty();
	}
	
	private String extractUserId(Optional<HttpCookie> cookieOpt) {
		if (cookieOpt.isEmpty()) return null;
		String token = cookieOpt.get().getValue();
		return jwtProvider.extractUserId(token);
	}
	
}
