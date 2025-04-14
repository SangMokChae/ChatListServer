package kr.co.dataric.chatlist.dto.read;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadCountMessage {
	
	private String roomId;
	private String msgId;
	private int readCount;
	
}
