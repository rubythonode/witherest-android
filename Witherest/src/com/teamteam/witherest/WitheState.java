package com.teamteam.witherest;

public class WitheState {
	
	private static WitheState witheState = null;
	public static final int INVALID_ID = -1;
	
	public static final int CREATE_ROOM = 1;
	public static final int JOIN_ROOM = 2;
	public static final int LEAVE_ROOM = 3;
	public static final int MODIFY_ROOM = 4;
	public static final int DELETE_ROOM = 5;
	
	public boolean mHaveChanged;
	public int mChangeType;
	public  boolean mMustAllLoaded;
	public  boolean mMustOneLoaded;
	
	public boolean mMustAllModified;
	public boolean mMustOneModified;
	
	public boolean mMustAllDeleted;
	public boolean mMustOneDeleted;
	
	public Object mObject;
	public  int mId;
	
	
	private WitheState(){
		mMustAllLoaded = false;
		mMustOneLoaded = false;
		mId = INVALID_ID;
		mObject = null;
	}
	
	public static WitheState getInstance(){
		if (witheState == null){
			witheState = new WitheState();
		}
		return witheState;
	}
		
	public void init() {
		mHaveChanged = false;
		mMustAllLoaded = false;
		mMustOneLoaded = false;
		mId = INVALID_ID;
		mObject = null;
	}
}
