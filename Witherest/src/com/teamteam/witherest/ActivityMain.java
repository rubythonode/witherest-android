package com.teamteam.witherest;

import com.evernote.client.android.EvernoteSession;
import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.FragmentNotice.FragmentRoomNewsAdapter;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.model.Session;

import com.teamteam.witherest.service.callback.UserServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;
import com.teamteam.witherest.service.internal.UserService;
import com.viewpagerindicator.R.anim;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

public class ActivityMain extends FragmentActivity implements UserServiceCallback{

	public static final int ACT_CREATE_ROOM = 0;
	public UserService userService;
	public  SimpleProgressDialog waitProgressDialog;
	
	public FragmentBottomContainer mBottomFragment;
	public FrameLayout mTouchContainer;
	public TranslateAnimation mFadeoutAnim;
	public TranslateAnimation mFadeinAnim;
	
	public static boolean isMyCheckRoomMustReloaded = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initInstance();
		initView();
		initListener();
	}

	private void initInstance() {
		userService = ServiceManager.getServiceManager().getUserService();
	}
	
	private void initView() {
		mTouchContainer = (FrameLayout)findViewById(R.id.activity_main_fragment);
		FragmentManager fragmentManger = getSupportFragmentManager();
		mBottomFragment = (FragmentBottomContainer)fragmentManger.findFragmentById(R.id.activity_main_bottomMenu);
		
		mFadeoutAnim = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF,1,Animation.RELATIVE_TO_SELF,1,
				Animation.RELATIVE_TO_SELF,1,Animation.RELATIVE_TO_SELF,0);
		
		mFadeinAnim = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF,1,Animation.RELATIVE_TO_SELF,0,
				Animation.RELATIVE_TO_SELF,1,Animation.RELATIVE_TO_SELF,1);
				
	}

	
	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		 if (e.getAction() == MotionEvent.ACTION_UP){
			showBottom();
		}else if( e.getAction() == MotionEvent.ACTION_MOVE){
			hideBottom();
		}
		super.dispatchTouchEvent(e);
		return true;
	}

	private void initListener() {}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.finish_confirm_title)
					.setMessage(R.string.finish_confirm_message)
					.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int which) {
									if (Session.getInstance().sessionStatus == Session.AUTHORIZED){
										logout();
									}else {
										finish();
									}
								}
							}).setNegativeButton(R.string.cancel, null).show();

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	
	protected void logout() {
		waitProgressDialog = new SimpleProgressDialog(this, 
				getString(R.string.wait_title),getString(R.string.logout_wait_message));
		waitProgressDialog.start();
		waitProgressDialog.show();
		userService.setOnUserCallback(this);
		userService.logout(Session.getInstance().user.userIndex, Session.getInstance().user.gcmId);
	}

	public void onUserServiceCallback(BaseResponseObject object) {
		if (waitProgressDialog.isShowing()){
			SimpleProgressDialog.end(waitProgressDialog);
		}
		if (object.resultCode == Service.RESULT_FAIL) {
			AndroUtils.showToastMessage(ActivityMain.this,R.string.logout_fail, Toast.LENGTH_SHORT);
			return;
		}
		
		switch(object.requestType){
		case Service.REQUEST_TYPE_LOGOUT:
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
				AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
				builder2.setTitle(R.string.tempo_network_error);
				builder2.setMessage(R.string.tempo_network_error_message);
				builder2.setCancelable(false);
				builder2.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Handler().postDelayed(new Runnable() {
									public void run() {
										logout();
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
			
			finish();
			break;
		}	
	}


	public void hideBottom() {
		FragmentTransaction fragTansaction = getSupportFragmentManager().beginTransaction();
		fragTansaction.setCustomAnimations(R.anim.bottom_fade_out, R.anim.botoom_fade_in);
		fragTansaction.hide(mBottomFragment);
		fragTansaction.commit();	
	}

	public void showBottom() {
		FragmentTransaction fragTansaction = getSupportFragmentManager().beginTransaction();
		fragTansaction.setCustomAnimations(R.anim.bottom_fade_out, R.anim.botoom_fade_in);
		fragTansaction.show(mBottomFragment);
		fragTansaction.commit();	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
	      case EvernoteSession.REQUEST_CODE_OAUTH:
	        if (resultCode == Activity.RESULT_OK) {
	          FragmentManager fragmentManager = getSupportFragmentManager();
	          Fragment fragment = fragmentManager.findFragmentById(R.id.activity_main_fragment);
	          if (fragment instanceof FragmentMyCheck){
	        	  fragment.onActivityResult(requestCode, resultCode, data);
	          }
	        }
	        break;
	    }
	}	
}