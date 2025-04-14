package kr.co.dataric.chatlist.service.friend;

import kr.co.dataric.chatlist.repository.friends.FriendRepository;
import kr.co.dataric.common.entity.Friends;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class FriendService {
	
	private final FriendRepository friendRepository;
	
	public Flux<Friends> getFriendList(String userId) {
		return friendRepository.findAcceptedFriendsByUserId(userId);
	}
	
}
