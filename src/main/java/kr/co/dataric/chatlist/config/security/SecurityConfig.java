package kr.co.dataric.chatlist.config.security;

import kr.co.dataric.chatlist.filter.jwt.JwtAuthenticationFilter;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.net.URI;
import java.util.List;

@Slf4j
@EnableWebFluxSecurity
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {
	
	private final JwtProvider jwtProvider;
	private final RedisService redisService;

	@Bean
	public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
		return http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
			.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
			.authorizeExchange(exchange -> exchange
				.pathMatchers("/favicon.ico", "/css/**", "/js/**", "/img/**", "/ws/chatList/**").permitAll()
				.anyExchange().authenticated()
			)
			.addFilterAt(new JwtAuthenticationFilter(jwtProvider, redisService), SecurityWebFiltersOrder.AUTHENTICATION)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((exchange, ex1) -> {
					if (!exchange.getResponse().isCommitted()) {
						exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER); // 303 See Other
						exchange.getResponse().getHeaders().setLocation(URI.create("/login"));
					}
					return exchange.getResponse().setComplete();
				})
				.accessDeniedHandler((exchange, denied) -> {
					if (!exchange.getResponse().isCommitted()) {
						exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER); // 303
						exchange.getResponse().getHeaders().setLocation(URI.create("/error?code=denied"));
					}
					return exchange.getResponse().setComplete();
				})
			)
			.build();
	}
	
}