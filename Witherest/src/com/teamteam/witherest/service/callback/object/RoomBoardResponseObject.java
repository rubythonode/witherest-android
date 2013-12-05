package com.teamteam.witherest.service.callback.object;

import java.util.ArrayList;

/**
 * @author Administrator
 *
 */
public class RoomBoardResponseObject extends BaseResponseObject {
	
	public String roomManagerNotice;
	public String roomManagerPurpose;
	public String roomManagerName;
	public int roomManagerId;
	public int totalBoardCount;
	public String roomManageImagePath;
	public ArrayList<Message> messageList;
	
	public String getRoomManageImagePath() {return roomManageImagePath;}
	public void setRoomManageImagePath(String roomManageImagePath) {this.roomManageImagePath = roomManageImagePath;}

	public String getRoomManagerNotice() {return roomManagerNotice;}
	public void setRoomManagerNotice(String roomManagerNotice) {this.roomManagerNotice = roomManagerNotice;}
	
	public int getTotalBoardCount() {return totalBoardCount;}
	public void setTotalBoardCount(int totalBoardCount) {this.totalBoardCount = totalBoardCount;}
	
	public ArrayList<Message> getMessageList() {return messageList;}
	public void setMessageList(ArrayList<Message> messageList) {this.messageList = messageList;	}
	
	public String getRoomManagerPurpose() {return roomManagerPurpose;}
	public void setRoomManagerPurpose(String roomManagerPurpose) {this.roomManagerPurpose = roomManagerPurpose;}
	
	public String getRoomManagerName() {return roomManagerName;}
	public void setRoomManagerName(String roomManagerName) {this.roomManagerName = roomManagerName;}
	
	public int getRoomManagerId() {return roomManagerId;}
	public void setRoomManagerId(int roomManagerId) {this.roomManagerId = roomManagerId;}

	public static class Message {
		public int messageId;
		public int writeId;
		public String writerNickname;
		public String writeTime;
		public String message;
		public boolean isReply;
		public int article_type;
		public int replyCount;
		public int parentId;
		public String writerImagePath;
		
		
		public int getReplyCount() {return replyCount;}
		public void setReplyCount(int replyCount) {this.replyCount = replyCount;}
		
		public int getMessageId() {return messageId;}
		public void setMessageId(int messageId) {this.messageId = messageId;}
		
		public int getWriteId() {return writeId;}
		public void setWriteId(int writeId) {this.writeId = writeId;}
		
		public String getWriterNickname() {return writerNickname;}
		public void setWriterNickname(String writerNickname) {this.writerNickname = writerNickname;}
		
		public String getWirteTime() {return writeTime;}
		public void setWirteTime(String wirteTime) {this.writeTime = wirteTime;}
		
		public String getMessage() {return message;}
		public void setMessage(String message) {this.message = message;}
		
		public boolean isReply() {return isReply;}
		public void setReply(boolean isReply) {this.isReply = isReply;}
		
		public int getArticle_type() {return article_type;}
		public void setArticle_type(int article_type) {this.article_type = article_type;}
		
		public int getParentId() {return parentId;}
		public void setParentId(int parentId) {this.parentId = parentId;}
		
		public String getWriterImagePath() {return writerImagePath;}
		public void setWriterImagePath(String writerImagePath) {this.writerImagePath = writerImagePath;}
		
	}
}
