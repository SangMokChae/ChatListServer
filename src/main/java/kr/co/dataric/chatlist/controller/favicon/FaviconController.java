package kr.co.dataric.chatlist.controller.favicon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FaviconController {
	
	@GetMapping("/favicon.ico")
	public Mono<Void> favicon() {
		return Mono.empty(); // 혹은 특정 favicon 반환
	}
	
}
