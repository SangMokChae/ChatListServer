package kr.co.dataric.chatlist.repository.friends;

import kr.co.dataric.common.entity.Friends;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface FriendRepository extends R2dbcRepository<Friends, Long> {

	@Query("SELECT * FROM friends WHERE user_id = :userId AND friend_status = 'ACCEPTED' ORDER BY friend_nickname")
	Flux<Friends> findAcceptedFriendsByUserId(String userId);
	
}
