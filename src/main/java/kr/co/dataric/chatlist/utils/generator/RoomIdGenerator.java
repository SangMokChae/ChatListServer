package kr.co.dataric.chatlist.utils.generator;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

@Component
public class RoomIdGenerator {
	
	public String generateRoomId(List<String> userList) {
		try {
			List<String> sortedUsers = userList.stream()
				.sorted()
				.toList();
			
			String base = String.join(":", sortedUsers);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
			
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (Exception e) {
			throw new RuntimeException("roomId 생성 실패", e);
		}
	}
	
}
