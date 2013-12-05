package com.teamteam.witherest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.OnClientCallback;
import com.evernote.client.android.EvernoteSession.EvernoteService;
import com.evernote.edam.type.Note;
import com.teamteam.customComponent.SimpleProgress;
import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.cacheload.ImageLoader;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.common.CommonUtils;
import com.teamteam.witherest.model.Session;
import com.teamteam.witherest.service.callback.RoomServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.callback.object.CheckRoomResponseObject;
import com.teamteam.witherest.service.callback.object.MyCheckResponseObject;
import com.teamteam.witherest.service.callback.object.RoomInfoResponseObject;
import com.teamteam.witherest.service.callback.object.MyCheckResponseObject.CheckRoom;
import com.teamteam.witherest.service.callback.object.RoomInfo;
import com.teamteam.witherest.service.external.EvernoteController;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.RoomService;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;

public class FragmentMyCheck extends Fragment implements RoomServiceCallback{
	
	public MyCheckResponseObject myMyCheckResponse;
	public ListView myCheckRoomLisView;
	private Activity mActivity;
	
	private View mMyCheckView;
	private TextView mNameTextView;
	private TextView mPurposeTextView;
	private TextView mMyCheckStartTextView;
	private TextView mMyCheckExposeTextView;
	private ImageView mProfileImgView;
	
	private SimpleProgressDialog waitProgressDialog;
	private SimpleProgressDialog evernoteDialog;
	
	public MyCheckListAdapter mMyCheckListAdapter;
	public int mCurCheckActionRoomId= -1;
	
	public int mRefreshPos;
	public TextView mRefeshBadge;
	
	public RoomService roomService;
	public ImageLoader imageLoader; 
	
	public int mSelectPosition;
	public int mCheckedPosition;
	
	public EvernoteController ever;
	
	public static final String TAG=" Fragment_MyCheck";
	

	
	/* Fragment Life Cycle */
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		imageLoader = new ImageLoader(getActivity().getApplicationContext());
		//imageLoader.clearCache();
		ever = new EvernoteController(getActivity());
		this.mActivity = activity;
		roomService = ServiceManager.getServiceManager().getRoomService();
		roomService.setOnRoomCallback(this);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		mMyCheckView = inflater.inflate(R.layout.fragment_mycheck, null);
		
		mProfileImgView = (ImageView)mMyCheckView.findViewById(R.id.mycheck_prifile_image);
		mNameTextView= (TextView)mMyCheckView.findViewById(R.id.mycheck_name_textview);
		mPurposeTextView= (TextView)mMyCheckView.findViewById(R.id.mycheck_purpose_textview);
		
		mMyCheckStartTextView = (TextView)mMyCheckView.findViewById(R.id.mycheck_star_textview);
		mMyCheckExposeTextView = (TextView)mMyCheckView.findViewById(R.id.mycheck_expose_textview);
		myCheckRoomLisView = (ListView)mMyCheckView.findViewById(R.id.mycheck_checklistview);
		
		mMyCheckView.findViewById(R.id.make_room_btn).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (Session.getInstance().sessionStatus == Session.NOT_AUTHORIZED){
					return;
				}
				Intent i = new Intent(mActivity, ActivityMakeRoom.class);
				startActivity(i);
			}
		});
		
		/*String url = null;
		if (!CommonUtils.isNullOrEmpty(Session.getInstance().user.profileImagePath)){
			url = Service.BASE_URL +  Session.getInstance().user.profileImagePath;
		}
		
		imageLoader.displayImage(url, profileImageView,ImageLoader.DEFAULT_PROFILE_IMAGE);*/
		return mMyCheckView;
	}
	
	
	public void loadMyCheckRooms(){
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
			
		RoomService roomService = ServiceManager.getServiceManager().getRoomService();
		roomService.setOnRoomCallback(this);
		roomService.getMyCheckRooms(String.valueOf(Session.getInstance().user.userIndex));
	}
	
	public void onRoomServiceCallback(BaseResponseObject object) {
		
		if (waitProgressDialog.isShowing()){
			waitProgressDialog.dismiss();
		}
		if (object.resultCode == Service.RESULT_FAIL) {
			return;
		}
		
		switch(object.requestType){
		case Service.REQUEST_TYPE_GET_MY_CHECKROOMS:
			
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.fatal_network_error);
				builder.setMessage(R.string.fatal_network_error_message);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {

							}
						});
				builder.create().show();
				return;
			}else if (object.resultCode == ConnectionErrorHandler.CONNECTION_TIMEOUT || object.resultCode == ConnectionErrorHandler.READ_TIMEOUT){
				AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
				builder2.setTitle(R.string.tempo_network_error);
				builder2.setMessage(R.string.tempo_network_error_message);
				builder2.setCancelable(false);
				builder2.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Handler().postDelayed(new Runnable() {
									public void run() {
										loadMyCheckRooms();
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
			
			myMyCheckResponse = (MyCheckResponseObject)object;
			
			if (ActivityMain.isMyCheckRoomMustReloaded == true){
				ActivityMain.isMyCheckRoomMustReloaded = false;
			}
			updateUi();
			setAdapter();
			break;
			
		case Service.REQUEST_TYPE_CHECK_ROOM:
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.fatal_network_error);
				builder.setMessage(R.string.fatal_network_error_message);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				builder.create().show();
				return;
			}else if (object.resultCode == ConnectionErrorHandler.CONNECTION_TIMEOUT || object.resultCode == ConnectionErrorHandler.READ_TIMEOUT){
				AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
				builder2.setTitle(R.string.tempo_network_error);
				builder2.setMessage(R.string.tempo_network_error_message);
				builder2.setCancelable(false);
				builder2.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Handler().postDelayed(new Runnable() {
									public void run() {
										if (mCurCheckActionRoomId != -1){
											checkRoom(mCurCheckActionRoomId);
										}
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
			CheckRoomResponseObject checkRoomResponse = (CheckRoomResponseObject)object;
			myMyCheckResponse.roomList.get(mRefreshPos).checkedMemberCount = checkRoomResponse.checkedMemberCount;
			myMyCheckResponse.roomList.get(mRefreshPos).checked = true;
			mCurCheckActionRoomId = -1;
			updateBadge();

			if (Session.getInstance().user.isEvernote && ever.isLoggedin()){
				createNote();	
			}
			
			break;
			
		case Service.REQUEST_TYPE_CANCEL_CHECK:
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.fatal_network_error);
				builder.setMessage(R.string.fatal_network_error_message);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				builder.create().show();
				return;
			}else if (object.resultCode == ConnectionErrorHandler.CONNECTION_TIMEOUT || object.resultCode == ConnectionErrorHandler.READ_TIMEOUT){
				AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
				builder2.setTitle(R.string.tempo_network_error);
				builder2.setMessage(R.string.tempo_network_error_message);
				builder2.setCancelable(false);
				builder2.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Handler().postDelayed(new Runnable() {
									public void run() {
										if (mCurCheckActionRoomId != -1){
											cancelCheck(mCurCheckActionRoomId);
										}
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
			CheckRoomResponseObject cancelCheckRoomResponse = (CheckRoomResponseObject)object;
			myMyCheckResponse.roomList.get(mRefreshPos).checkedMemberCount = cancelCheckRoomResponse.checkedMemberCount;
			myMyCheckResponse.roomList.get(mRefreshPos).checked = false;
			mCurCheckActionRoomId = -1;
			updateBadge();
			break;
			
		case Service.REQUEST_TYPE_GET_ROOMINFO:
			WitheState state = WitheState.getInstance();
			if (state.mChangeType == WitheState.CREATE_ROOM){
				RoomInfo roomInfo = ((RoomInfoResponseObject)object).roomInfo;
				addRoom(roomInfo);
			}else if (state.mChangeType == WitheState.MODIFY_ROOM){
				RoomInfo roomInfo = ((RoomInfoResponseObject)object).roomInfo;
				modifyRoom(roomInfo);
			}else if (state.mChangeType == WitheState.JOIN_ROOM){
				RoomInfo roomInfo = ((RoomInfoResponseObject)object).roomInfo;
				addRoom(roomInfo);
			}
			break;
		}
	}
	
	public void setAdapter(){
		mMyCheckListAdapter = new MyCheckListAdapter(mActivity,R.layout.mycheck_list_item_row, 
				myMyCheckResponse.roomList, imageLoader);
		myCheckRoomLisView.setAdapter(mMyCheckListAdapter);
		myCheckRoomLisView.setSelection(mSelectPosition != ListView.INVALID_POSITION ? mSelectPosition : 0);
	}
	
	
	public void updateBadge(){
		boolean  isRoomCompleted = myMyCheckResponse.roomList.get(mRefreshPos).checkedMemberCount >=myMyCheckResponse.roomList.get(mRefreshPos)
			.curMemberCount;
		mRefeshBadge.setBackgroundResource(isRoomCompleted? R.drawable.main_checknumber_complete_bg:R.drawable.main_checknumber_bg);
		mRefeshBadge.setPadding(1, 1, 1,1);
		mRefeshBadge.setText(CommonUtils.int2string( myMyCheckResponse.roomList.get(mRefreshPos).checkedMemberCount));
	}
	
	
	public void checkRoom(int roomId){
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		
		RoomService roomService = ServiceManager.getServiceManager().getRoomService();
		roomService.setOnRoomCallback(this);
		roomService.checkRoom(roomId);
	}
	
	public void cancelCheck(int roomId){
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		
		RoomService roomService = ServiceManager.getServiceManager().getRoomService();
		roomService.setOnRoomCallback(this);
		roomService.cancelCheckRoom(roomId);
		
	}
	
	public void showEvernoteWaitDialog(String message){
		evernoteDialog = new SimpleProgressDialog(getActivity(), getActivity().getString(R.string.wait_title),message);
		evernoteDialog.start();
		evernoteDialog.show();
		
	}
	
	public void dismissEvernoteWaitDialog(){
		if (waitProgressDialog.isShowing()){
			waitProgressDialog.dismiss();
		}
		SimpleProgressDialog.end(evernoteDialog);
	}
	
	//에버노트에 체그 내용을 기록한다.
	private void createNote() {
		if (mCheckedPosition == -1) return;
		showEvernoteWaitDialog(AndroUtils.getStringFromResource(getActivity(), R.string.is_saving_evernote));
		String roomName = myMyCheckResponse.roomList.get(mCheckedPosition).roomTitle;
		String time = myMyCheckResponse.roomList.get(mCheckedPosition).alarmTime;
			
		ever.createSimpleNote(AndroUtils.getStringFromResource(getActivity(),R.string.evernote_saving_title), roomName, 
				Session.getInstance().user.nickName, time,new OnClientCallback<Note>() {
					@Override
					public void onSuccess(Note data) {
						dismissEvernoteWaitDialog();
						mCheckedPosition = -1; 
						 Toast.makeText(getActivity(), R.string.note_saved, Toast.LENGTH_LONG).show();
					}
					
					@Override
					public void onException(Exception exception) {
						dismissEvernoteWaitDialog();
						Log.e(TAG, "Error saving note", exception);
				        Toast.makeText(getActivity(), R.string.error_saving_note, Toast.LENGTH_LONG).show();
						
					}
				});
	}
	
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null){
			myCheckRoomLisView.setSelectionFromTop(savedInstanceState.getInt("position"), 0);			
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();	
		waitProgressDialog = new SimpleProgressDialog(getActivity(), getActivity().getString(R.string.wait_title),
				getString(R.string.wait_message));
		waitProgressDialog.start();
		
		if (myMyCheckResponse == null || ActivityMain.isMyCheckRoomMustReloaded == true){
			loadMyCheckRooms();
		}else {
			updateUi();
			setAdapter();
		}
	}
	
	@Override
	public void onPause() {
		super.onResume();
		SimpleProgressDialog.end(waitProgressDialog);
		mSelectPosition = myCheckRoomLisView.getFirstVisiblePosition();

	}

	public void updateUi(){
		if (!CommonUtils.isNullOrZeroOrEmpty(Session.getInstance().user.nickName)){
			mNameTextView.setText(Session.getInstance().user.nickName);
		}
		
		if (!CommonUtils.isNullOrZeroOrEmpty(Session.getInstance().user.purpose)){
			mPurposeTextView.setText(Session.getInstance().user.purpose);
		}
		
		String imageUrl = null;
		if (!CommonUtils.isNullOrZeroOrEmpty(Session.getInstance().user.profileImagePath)){
			imageUrl = Service.BASE_URL + Session.getInstance().user.profileImagePath;
		}
		
		imageLoader.displayImage(imageUrl,mProfileImgView, ImageLoader.DEFAULT_PROFILE_IMAGE);
		mMyCheckExposeTextView.setText(myMyCheckResponse.createRoomCount + " / " + myMyCheckResponse.joinRoomCount);
		mMyCheckStartTextView.setText(myMyCheckResponse.userStarCount+"");
		WitheState.getInstance().init();
	}
	
	
	public void addRoom(RoomInfo roomInfo){
		ArrayList<CheckRoom> roomList = myMyCheckResponse.roomList;
		
		CheckRoom checkRoom = new CheckRoom();
		checkRoom.roomId = roomInfo.roomId;
		checkRoom.roomOwner = Session.getInstance().user.userIndex;
		checkRoom.roomTitle = roomInfo.roomTitle;
		checkRoom.roomPurpose = roomInfo.roomPurpose;
		checkRoom.roomImagePath = roomInfo.roomImagePath;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		checkRoom.startDate = format.format(roomInfo.startDate);
		checkRoom.endDate = format.format(roomInfo.endDate);
		checkRoom.checkedMemberCount = 0;
		checkRoom.maxMemberCount = roomInfo.maxMemberCount;
		checkRoom.curMemberCount = 1;
		checkRoom.checked = false;
		checkRoom.alarmTime = roomInfo.alarmHour + ":" + roomInfo.alarmMin;
		checkRoom.alarmLevel = roomInfo.alarmLevel;
		
		if (WitheState.getInstance().mChangeType == WitheState.CREATE_ROOM){
			myMyCheckResponse.createRoomCount = myMyCheckResponse.createRoomCount + 1;	
		}else if (WitheState.getInstance().mChangeType == WitheState.JOIN_ROOM){
			myMyCheckResponse.joinRoomCount = myMyCheckResponse.joinRoomCount +1;
		}else if (WitheState.getInstance().mChangeType == WitheState.LEAVE_ROOM){
			myMyCheckResponse.joinRoomCount = myMyCheckResponse.joinRoomCount -1;
		}
		
		roomList.add(checkRoom);
		updateUi();
		refreshList();
		myCheckRoomLisView.setSelection(mMyCheckListAdapter.getCount()-1);
	}
	
	private void modifyRoom(RoomInfo roomInfo) {
		ArrayList<CheckRoom> roomList = myMyCheckResponse.roomList;
		int modifyRoomNo = -1;
		for (int i =0; i <roomList.size(); i++ ){
			CheckRoom checkRoom = roomList.get(i);
			if (checkRoom.roomId == roomInfo.roomId){
				modifyRoomNo = i;
				break;
			}
		}
		CheckRoom checkRoom = roomList.get(modifyRoomNo);
		
		checkRoom.roomId = roomInfo.roomId;
		checkRoom.roomOwner = Session.getInstance().user.userIndex;
		checkRoom.roomTitle = roomInfo.roomTitle;
		checkRoom.roomPurpose = roomInfo.roomPurpose;
		checkRoom.roomImagePath = roomInfo.roomImagePath;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		checkRoom.startDate = format.format(roomInfo.startDate);
		checkRoom.endDate = format.format(roomInfo.endDate);
		checkRoom.checkedMemberCount = 0;
		checkRoom.maxMemberCount = roomInfo.maxMemberCount;
		checkRoom.curMemberCount = 1;
		checkRoom.checked = false;
		checkRoom.alarmTime = roomInfo.alarmHour + ":" + roomInfo.alarmMin;
		checkRoom.alarmLevel = roomInfo.alarmLevel;
		
		//이미지가 변경될 수도 있으니. 해당 URL 의 캐시를 삭제한다.
		//imageLoader.removeMemoryCache(Service.BASE_URL + roomInfo.roomImagePath);
		imageLoader.removeFileAndMemoryCache(Service.BASE_URL + roomInfo.roomImagePath);
		WitheState.getInstance().init();
		refreshList();
		
	}

	private void deleteRoomFromList(int id) {
		ArrayList<CheckRoom> roomList = myMyCheckResponse.roomList;
		int deleteRoomNo = -1;
		for (int i =0; i <roomList.size(); i++ ){
			CheckRoom checkRoom = roomList.get(i);
			if (checkRoom.roomId == id){
				deleteRoomNo = i;
				break;
			}
		}
		if (deleteRoomNo !=-1){
			roomList.remove(deleteRoomNo);
			WitheState.getInstance().init();
			refreshList();
		}
	}
   
	private void refreshList() {
		mMyCheckListAdapter.notifyDataSetChanged();
		myCheckRoomLisView.setSelection(mSelectPosition != ListView.INVALID_POSITION ? mSelectPosition : 0);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		 outState.putInt("position",mSelectPosition);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
	}

	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
	      case EvernoteSession.REQUEST_CODE_OAUTH:
	    	  if (resultCode == Activity.RESULT_OK) {
	    	  AndroUtils.showToastMessage(getActivity(), R.string.evernote_login_ok, Toast.LENGTH_SHORT);
	    	  createNote();
	    	  }else {
	    		  AndroUtils.showToastMessage(getActivity(), R.string.evernote_login_fail, Toast.LENGTH_SHORT);
	    	  }
	    	  break;
		}
	}

	public void getRoomById(int roomId){
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		roomService.setOnRoomCallback(this);
		roomService.getRoomInfo(roomId);
	}
	
	
	public class MyCheckListAdapter extends BaseAdapter{
		Context  context ; 
		int resId ;
		LayoutInflater inflate;
		ArrayList<CheckRoom> roomList;
		int badge1;
		int badge2;
		ImageLoader imageLoader;
		
		public MyCheckListAdapter(Activity act, int layoutRes, ArrayList<CheckRoom> roomList, ImageLoader imageLoader){
			this.context = act;
			this.resId = layoutRes;
			this.roomList = roomList;
			inflate = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.badge1 = R.drawable.main_checknumber_bg;
			this.badge2 = R.drawable.main_checknumber_complete_bg;
			this.imageLoader =imageLoader;
			//this.imageLoader.clearCache();
			
		}
		public int getCount() {
			// TODO Auto-generated method stub
			return this.roomList.size();
		}

		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return this.roomList.get(position);
		}

		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final int pos = position;
			MyCheckRoomViewHolder viewHolder;
			
			if (convertView == null){
				convertView = inflate.inflate(resId,parent, false);
				viewHolder = new MyCheckRoomViewHolder();			
				viewHolder.image = (ImageView)convertView.findViewById(R.id.list_image);
				viewHolder.joinInfo =  (TextView)convertView.findViewById(R.id.mycheck_room_joincount_textview);
				viewHolder.myCheck = (CheckBox)convertView.findViewById(R.id.mycheck_check_btn);
				viewHolder.purpose  = (TextView)convertView.findViewById(R.id.mycheck_room_purpose_textview);
				viewHolder.term = (TextView)convertView.findViewById(R.id.mycheck_room_team_textview);
				viewHolder.title = (TextView)convertView.findViewById(R.id.mycheck_room_title_textview);
				viewHolder.roomDetailBtn =  convertView.findViewById(R.id.mycheck_row_left_contaniner);
				viewHolder.alarmTimeBadge = (TextView)convertView.findViewById(R.id.alramtime_badge);
				viewHolder.checkBadge = (TextView)convertView.findViewById(R.id.mycheck_badge);
				convertView.setTag(viewHolder);
			}else {
				viewHolder = (MyCheckRoomViewHolder)convertView.getTag();
			}

		
			final TextView cBadge =viewHolder.checkBadge;
			viewHolder.image.setImageBitmap(null);
			String url  = null; 
			
			//이미지가 없을 경우 이미지 경로가 "0"으로 오는 경우가 있어 아래와 같이 코드를 적용한다.
			if (!CommonUtils.isNullOrEmpty(roomList.get(position).roomImagePath) && !"0".equals(roomList.get(position).roomImagePath)){
				url = Service.BASE_URL + roomList.get(position).roomImagePath;
			}
			imageLoader.displayImage(url, viewHolder.image, ImageLoader.DEFAULT_ROOM_IMAGE);
			
			viewHolder.alarmTimeBadge.setVisibility(View.VISIBLE);
			if (roomList.get(position).alarmLevel == RoomInfo.ROOM_ALARM_OK){
				viewHolder.alarmTimeBadge.setText(roomList.get(position).alarmTime);
			}else {
				viewHolder.alarmTimeBadge.setVisibility(View.INVISIBLE);
			}
			
			viewHolder.title.setText(roomList.get(position).roomTitle);
			viewHolder.purpose.setText(roomList.get(position).roomPurpose);
			viewHolder.term.setText(roomList.get(position).startDate + " ~ " + roomList.get(position).endDate );
			viewHolder.joinInfo.setText(roomList.get(position).curMemberCount + "/" +roomList.get(position).maxMemberCount );
			
			boolean  isRoomCompleted = roomList.get(pos).checkedMemberCount >=roomList.get(pos).curMemberCount;
			cBadge.setBackgroundResource(isRoomCompleted? badge2:badge1);
			cBadge.setPadding(1, 1, 1,1);
			cBadge.setText(CommonUtils.int2string(roomList.get(pos).checkedMemberCount));

	    	viewHolder.roomDetailBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					Intent i = new Intent(getActivity(), ActivityRoomDetail.class);
					i.putExtra("roomId", roomList.get(pos).roomId);
					i.putExtra("roomTitle", roomList.get(pos).roomTitle);
					i.putExtra("roomOwner", roomList.get(pos).roomOwner);
					startActivity(i);
				}
			});
			
	    	viewHolder.myCheck.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					CheckBox check = (CheckBox)view;
					Log.v(pos + ":" +roomList.get(pos).roomTitle+  " 체크상태", check.isChecked() + "");
					
					mRefreshPos = pos;
					mRefeshBadge = cBadge;
					mCurCheckActionRoomId = roomList.get(pos).roomId;
					
					if (check.isChecked()){
						mCheckedPosition = pos;
						checkRoom(roomList.get(pos).roomId);
					}else {
						cancelCheck(roomList.get(pos).roomId);
					}
				}
			});
	    	
	    	viewHolder.myCheck.setChecked(roomList.get(position).checked);
			return convertView;
		}
	}
	
	public class MyCheckRoomViewHolder {
		public ImageView image; 
		public TextView joinInfo; 
		public CheckBox myCheck;
		public TextView purpose; 
		public TextView term; 
		public TextView title; 
		public View roomDetailBtn; 
		public TextView alarmTimeBadge; 
		public TextView checkBadge;
	}
	
	
  
}