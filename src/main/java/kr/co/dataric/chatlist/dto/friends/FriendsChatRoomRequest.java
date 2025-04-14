package kr.co.dataric.chatlist.dto.friends;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendsChatRoomRequest {
	
	private String userId;
	private String friendId;
	
}
