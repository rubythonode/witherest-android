package com.teamteam.witherest.model;

import java.util.ArrayList;

import com.teamteam.witherest.common.CommonUtils;

import android.util.Log;

public class User {
	public int userIndex;
	public String id;
	public String password;
	public String nickName;
	public String purpose;
	public String gcmId;
	public String profileImagePath;
	public ArrayList<Category> categories  = new ArrayList<Category>();
	public String myCategoris;
	
	public boolean isRoomTimeNotice;
	public boolean isRoomManagerNotice;
	public boolean isReplyNotice;
	public boolean isWitherestNotice;
	public boolean isEvernote;
	
	 public String alarmParam;

	public boolean[] alarmArr = new boolean[5];
	
	public static int ROOM_MANAGER_ALARM_INDEX = 0; 
	public static int ROOM_TIME_ARLAM_INDEX =1;
	public static int REPLY_ALARM_INDEX = 2;
	public static int WITHEREST_ALARM_INDEX = 3;
	public static int EVERNOTE_ALARM_INDEX = 4;
	
	public User(){}

	public int getUserIndex() {return userIndex;}
	public void setUserIndex(int userIndex) {this.userIndex = userIndex;}
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	public String getPassword() {return password;}
	public void setPassword(String password) {this.password = password;}
	public String getNickName() {return nickName;}
	public void setNickName(String nickName) {this.nickName = nickName;}
	public String getPurpose() {return purpose;}
	public void setPurpose(String purpose) {this.purpose = purpose;}
	public String getProfileImagePath() {return profileImagePath;}
	public void setProfileImagePath(String profileImagePath) {this.profileImagePath = profileImagePath;}
	public String getMyCategoris() {return myCategoris;}

	public void setMyCategoris(String myCategoris) {
		if (CommonUtils.isNullOrEmpty(myCategoris)){
			myCategoris = null;
			return;
		}
		categories.clear();
		this.myCategoris = myCategoris;
		AppCache cache = AppCache.getInstance();
		ArrayList<Category> appCategory = cache.getAppCategory();

		int length = myCategoris.length();
		for (int i = 0; i <length; i++){
			int categoryId = Integer.parseInt(String.valueOf(myCategoris.charAt(i)));
			for (Category cat : appCategory){
				if (categoryId == cat.categoryId){
					 categories.add(new Category(cat.categoryId,cat.categoryName,cat.categoryDescription));
					break;
				}
			}
		}		
	}
	
	


	public void setIsRoomTimeNotice(boolean isRoomTimeNotice) {
		this.isRoomTimeNotice = isRoomTimeNotice;
		this.alarmArr[ROOM_TIME_ARLAM_INDEX] = isRoomTimeNotice;
		updateAlarmParam();
	}

	public void setIsRoomManagerNotice(boolean isRoomManagerNotice) {
		this.isRoomManagerNotice = isRoomManagerNotice;
		this.alarmArr[ROOM_MANAGER_ALARM_INDEX] = isRoomManagerNotice;
		updateAlarmParam();
	}
	
	public void setIsReplyNotice(boolean isReplyNotice) {
		this.isReplyNotice = isReplyNotice;
		this.alarmArr[REPLY_ALARM_INDEX] = isReplyNotice;
		updateAlarmParam();
	}
	
	public void setIsWitherestNotice(boolean isWitherestNotice) {
		this.isWitherestNotice = isWitherestNotice;
		this.alarmArr[WITHEREST_ALARM_INDEX] = isWitherestNotice;
		updateAlarmParam();
	}
	
	public void setIsEvernote(boolean isEvernote){
		this.isEvernote = isEvernote;
		this.alarmArr[EVERNOTE_ALARM_INDEX] = isEvernote;
		updateAlarmParam();
	}

	private void updateAlarmParam() {
		String alarmTmp = "";
		for (int i = 0; i <alarmArr.length; i++){
			boolean f = alarmArr[i];
			if (f == true){
				alarmTmp +="1";
			}else {
				alarmTmp +="0";
			}
			if (i < alarmArr.length-1){
				alarmTmp +="|";
			}
		}
		alarmParam = alarmTmp;
	}
	
	public String getAlarmParam() {
		return alarmParam;
	}

	public void setAlarmParam(String alarmParam) {
		this.alarmParam = alarmParam;
	}
	
	public String getGcmId() {return gcmId;}
	public void setGcmId(String gcmId) {this.gcmId = gcmId;}

}