package com.teamteam.witherest;

import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.alarm.WitherestAlarms;
import com.teamteam.witherest.alarm.WitherestAlarms.Alarm;
import com.teamteam.witherest.alarm.WitherestAlarms.AlarmCancelAction;
import com.teamteam.witherest.alarm.WitherestAlarms.AlarmRegisterAction;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.common.CommonUtils;
import com.teamteam.witherest.model.Session;
import com.teamteam.witherest.service.callback.RoomServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.RoomService;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityRoomDetail extends FragmentActivity implements RoomServiceCallback{

	public static FragmentRoomWith mRoomWithFragment ;
	public static FragmentRoomBoard mRoomBoardFragment;
	
	public int mRoomId;
	public String mRoomTitle;

	public RadioGroup mRadioGroup;
	public TextView mBehaviorBtn;
	
	public int mBehaviorType;
	
	public static final int NOT_SETTING = 0;
	public static final int OWNER = 1;    // 방의 소유자 
	public static final int MEMBER_JOINED = 2;     
	public static final int MEMBER_NOT_JOINED = 3;
	public static final int NOT_MEMBER = 4; 
	
	public SimpleProgressDialog waitProgressDialog;
	
	public static final int DEFAULT_PAGE = 1;
	
	public static final int ROOM_MODIFY = 100;
	
	/*룸위드 화면이 아닌 다른 곳에서 방에 참여, 혹은 탈퇴, 방 수정할 경우 룸위드 의 정보도 갱신해야 하기 때문에
	룸위드의 갱신 여부를 나타내는 플래그 변수*/
	public   boolean mIsRoomwithMustReloaded = false;
	
	//롬보드에서 게시물을 보고 다시 룸보드로 돌아왔을 때 댓글의 추가 삭제에 대해서 원글 리스트 갱신 여부를 판단하는 requestCode
	public static final int ACTION_VIEW_CONTENT = 0;
	//회원이 아닌 상태에서 방 참가버튼을 눌르고, 회원 가입으로 전환 후 다시 돌아왔을 때 갱신을 위한 requestCode
	public static final int MEMBER_JOIN_FROM_ROOM = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.activity_room_detail);
		Intent i = getIntent();
		mRoomId = i.getIntExtra("roomId",-1);
		mRoomTitle = i.getStringExtra("roomTitle");
		if (mRoomId == -1 || CommonUtils.isNullOrEmpty(mRoomTitle)){
			AndroUtils.showToastMessage(this, R.string.inappropriate_path, Toast.LENGTH_SHORT);
			return;
		}
		initInstance();
		initView();
		initListener();
		changeFragment(mRoomWithFragment);
		Log.v("RoomDetailActivity", "onCreate 호출됨");
	}


	private void initInstance() {
		mBehaviorType = NOT_SETTING;;
		mRoomWithFragment = FragmentRoomWith.newInstance(mRoomId, DEFAULT_PAGE);
		mRoomBoardFragment = FragmentRoomBoard.newInstance(mRoomId,DEFAULT_PAGE);
	}

	private void initView() {
		mRadioGroup = (RadioGroup)findViewById(R.id.room_detail_radio_group);
		TextView roomTitleView = (TextView)findViewById(R.id.room_detail_titl_textview);
		mBehaviorBtn = (TextView)findViewById(R.id.room_detail_top_button);
		roomTitleView.setText(mRoomTitle);
	}

	private void initListener() {
		mRadioGroup.setOnCheckedChangeListener(mRadioCheck);
		mBehaviorBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				switch(mBehaviorType){
				case OWNER:
					Intent i = new Intent(ActivityRoomDetail.this, ActivityMakeRoom.class);
					i.putExtra("mode", ActivityMakeRoom.ROOM_MODIFY);
					i.putExtra("roomId", mRoomId);
					i.putExtra("roomTitle", mRoomTitle);
					
					/*방 수정하기화면에는 삭제 버튼도 있는데. 삭제 가능 여부는 방에 멤버가 있는지 없는지로 판단하기 때문에
					방의 현재 인원수도 전달해준다*/
					
					i.putExtra("curMemberCount", mRoomWithFragment.roomWithResponse.checkRoomWith.curMemberCount);
					startActivityForResult(i, ROOM_MODIFY);
					break;
					
				case MEMBER_JOINED:
					//방탈퇴
					leaveRoom();
					break;
					
				case MEMBER_NOT_JOINED:
					joinRoom();
					break;
					
				case NOT_MEMBER:
					showMemberGuide();
					break;
					
				default:
				}
			}
		});
	}
	
	private void showMemberGuide() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.member_guide_title);
		builder.setMessage(R.string.member_guide_message);
		builder.setCancelable(false);
		builder.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent(ActivityRoomDetail.this, ActivitySignup.class);
						ActivityRoomDetail.this.startActivityForResult(i, MEMBER_JOIN_FROM_ROOM);
					}
				});

		builder.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {}
				});

		builder.create().show();
		
	}
	public void changeBehaviorType(int type){
		if (type == NOT_SETTING) return;
		mBehaviorType = type;
		
		switch(mBehaviorType){
		case OWNER:
			mBehaviorBtn.setText(R.string.room_modify);
			break;
			
		case MEMBER_JOINED:
			mBehaviorBtn.setText(R.string.room_withdraw);
			break;
			
		case MEMBER_NOT_JOINED:
			mBehaviorBtn.setText(R.string.room_join);
			break;
			
		case NOT_MEMBER:
			mBehaviorBtn.setText(R.string.room_join);
			break;
		default:
		}
	}
	
	RadioGroup.OnCheckedChangeListener mRadioCheck = new RadioGroup.OnCheckedChangeListener() {
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			if (group.getId() ==R.id.room_detail_radio_group){
				switch(checkedId){
				case R.id.room_with_radio_btn:
					changeFragment(mRoomWithFragment);
				break;
				case R.id.room_board_radio_btn:
					changeFragment(mRoomBoardFragment);
				break;
					
				}
			}
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ( resultCode != RESULT_OK)
			return;
		
		switch (requestCode) {
		case ACTION_VIEW_CONTENT:
			mRoomBoardFragment.onActivityResult(requestCode, resultCode, data);
			break;
		case MEMBER_JOIN_FROM_ROOM:
			changeBehaviorType(MEMBER_NOT_JOINED);
			break;
		case ROOM_MODIFY:
			markRoomReloaded();
			break;
		}
	}
	
	private void changeFragment(Fragment fragment) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.activity_roomdetail_content_fragment ,fragment);
		// 프래그먼트의 교체를 애니메이션화 한다.
		// 애니메이션을 할 경우, 빠르게 교체 할 경우, 프래그먼트 전환에 문제가 생길 수 있으므로 주석처리한다.
		//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		SimpleProgressDialog.end(waitProgressDialog);
	}


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		waitProgressDialog = new SimpleProgressDialog(this, getString(R.string.wait_title),getString(R.string.wait_message));
		waitProgressDialog.start();
	}


	private void joinRoom() {
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		RoomService roomService = ServiceManager.getServiceManager().getRoomService();
		roomService.setOnRoomCallback(this);
		roomService.joinRoom(mRoomId);
	}

	private void leaveRoom() {
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		RoomService roomService = ServiceManager.getServiceManager().getRoomService();
		roomService.setOnRoomCallback(this);
		roomService.leaveRoom(mRoomId);
	}
	
	public void onRoomServiceCallback(BaseResponseObject object) {
		if (waitProgressDialog.isShowing()){
			waitProgressDialog.dismiss();
		}
		
		if (object.resultCode == Service.RESULT_FAIL) {
			return;
		}
		
		WitherestAlarms alarms = null;
		
		switch(object.requestType){
		case Service.REQUEST_TYPE_GET_ROOM_WITH:
			 break;
			 
		case Service.REQUEST_TYPE_JOIN_ROOM:
			
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.fatal_network_error);
				builder.setMessage(R.string.fatal_network_error_message);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						});
				builder.create().show();
				return;
			}else if (object.resultCode == ConnectionErrorHandler.CONNECTION_TIMEOUT || object.resultCode == ConnectionErrorHandler.READ_TIMEOUT){
				AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
				builder2.setTitle(R.string.tempo_network_error);
				builder2.setMessage(R.string.tempo_network_error_message);
				builder2.setCancelable(false);
				builder2.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Handler().postDelayed(new Runnable() {
									public void run() {
										joinRoom();
									}
								}, 100);
							}
						});

				builder2.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						});

				builder2.create().show();
				return;
			}
			
			markRoomReloaded();
			ActivityMain.isMyCheckRoomMustReloaded = true;
			changeBehaviorType(MEMBER_JOINED);
			
			WitheState.getInstance().init();
			WitheState.getInstance().mChangeType = WitheState.JOIN_ROOM;
			WitheState.getInstance().mHaveChanged = true;
			WitheState.getInstance().mId = mRoomId;
			WitheState.getInstance().mMustOneLoaded = true;
			
			//참여한 방의 알람을 등록한다.
			alarms = new WitherestAlarms(this);
			Alarm alarm = new Alarm();
			alarm.userId = Session.getInstance().user.id;
			alarm.roomId = mRoomId;
			alarm.roomName = mRoomTitle;;
			alarm.roomPurpse = mRoomWithFragment.roomWithResponse.checkRoomWith.roomPurpose;
			alarm.alarmTime = mRoomWithFragment.roomWithResponse.checkRoomWith.alarmTime;
			alarm.alarmEnabled =mRoomWithFragment.roomWithResponse.checkRoomWith.alarmLevel;
			alarm.userRoomTimeOption = Session.getInstance().user.isRoomTimeNotice ? 1: 0;
			
			alarms.registerAlarm(alarm, AlarmRegisterAction.INSERT_AFTER_ALARM_START);
			alarms.close();
			break;
			 
		case Service.REQUEST_TYPE_LEAVE_ROOM:
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.fatal_network_error);
				builder.setMessage(R.string.fatal_network_error_message);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						});
				builder.create().show();
				return;
			}else if (object.resultCode == ConnectionErrorHandler.CONNECTION_TIMEOUT || object.resultCode == ConnectionErrorHandler.READ_TIMEOUT){
				AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
				builder2.setTitle(R.string.tempo_network_error);
				builder2.setMessage(R.string.tempo_network_error_message);
				builder2.setCancelable(false);
				builder2.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Handler().postDelayed(new Runnable() {
									public void run() {
										leaveRoom();
									}
								}, 100);
							}
						});

				builder2.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						});

				builder2.create().show();
				return;
			}
			markRoomReloaded();
			ActivityMain.isMyCheckRoomMustReloaded = true;
			
			changeBehaviorType(MEMBER_NOT_JOINED);
			WitheState.getInstance().init();
			WitheState.getInstance().mChangeType = WitheState.LEAVE_ROOM;
			WitheState.getInstance().mHaveChanged = true;
			WitheState.getInstance().mId = mRoomId;
			WitheState.getInstance().mMustOneDeleted = true;
			
			//탈퇴한 방의 알람을 해제한다.
			alarms = new WitherestAlarms(this);
			alarms.unregisterlAlarm(mRoomId, AlarmCancelAction.CANCEL_AFTER_DELETE);
			alarms.close();
			break;
		}		
		
	}

	private void markRoomReloaded() {
		mIsRoomwithMustReloaded = true;
	}
}
