package com.teamteam.witherest;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.teamteam.customComponent.SimpleProgress;
import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.alarm.WitherestAlarms;
import com.teamteam.witherest.alarm.WitherestAlarms.Alarm;
import com.teamteam.witherest.alarm.WitherestAlarms.AlarmRegisterAction;
import com.teamteam.witherest.cacheload.ImageLoader;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.common.CommonUtils;
import com.teamteam.witherest.model.AppCache;
import com.teamteam.witherest.model.AppInfo;
import com.teamteam.witherest.model.Category;
import com.teamteam.witherest.model.Session;
import com.teamteam.witherest.model.User;
import com.teamteam.witherest.model.UserOption;
import com.teamteam.witherest.model.UserProfile;
import com.teamteam.witherest.service.callback.CategoryServiceCallback;
import com.teamteam.witherest.service.callback.RoomServiceCallback;
import com.teamteam.witherest.service.callback.UserServiceCallback;
import com.teamteam.witherest.service.callback.VersionServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.callback.object.CategoryResponseObject;
import com.teamteam.witherest.service.callback.object.LoginResponseObject;
import com.teamteam.witherest.service.callback.object.MyCheckResponseObject;
import com.teamteam.witherest.service.callback.object.VersionResponseObject;
import com.teamteam.witherest.service.callback.object.MyCheckResponseObject.CheckRoom;
import com.teamteam.witherest.service.external.GCMController;
import com.teamteam.witherest.service.internal.CategoryService;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.RoomService;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;
import com.teamteam.witherest.service.internal.UserService;
import com.teamteam.witherest.service.internal.VersionService;

public class ActivitySplash extends Activity implements UserServiceCallback, 
		VersionServiceCallback,CategoryServiceCallback {

	SimpleProgressDialog dialog;
	SimpleProgress progress;
	GCMController gcmController; 
	
	public TextView mProgressTextview;
	public String mUserId;
	public String mPassword;
	
	Animation mAnim;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*requestWindowFeature(Window.FEATURE_NO_TITLE);*/
		setContentView(R.layout.activity_splash);
		
		gcmController = new GCMController(this);
		progress = new SimpleProgress(this);
		mProgressTextview= (TextView)findViewById(R.id.progress_text);
		
		mAnim = new AlphaAnimation(0.0f, 1.0f);
		mAnim.setDuration(400); 
		mAnim.setStartOffset(20);
		mAnim.setRepeatMode(Animation.REVERSE);
		mAnim.setRepeatCount(Animation.INFINITE);
		
		new Handler().postDelayed(new Runnable() {
			public void run() {
				mProgressTextview.startAnimation(mAnim);
				progress.start();
				initializeApp();
				checkVersion();
			}
		}, 100);
	}

	public void showProgressText(String progress){
		mProgressTextview.setText(progress);
	}
	
	private void initializeApp() {
		new ImageLoader(getApplicationContext()).clearCache();
		showProgressText(getResources().getString(R.string.progress_initialize));
		AppInfo appInfo = AppInfo.getInstance();
		appInfo.loadAppInfo();
		
		Session session = Session.getInstance();
		session.initialize();
	}

	private void checkVersion() {
		showProgressText(getResources().getString(R.string.progress_version));
		ServiceManager manager = ServiceManager.getServiceManager();
		VersionService versionService = manager.getVersionService();
		versionService.setOnVersionCallback(this);
		versionService.checkVersion();
	}

	private void getCategories(){
		showProgressText(getResources().getString(R.string.progress_category));
		ServiceManager manager = ServiceManager.getServiceManager();
		CategoryService categoryService = manager.getCategoryService();
		categoryService.setOnCategoryCallback(this);
		categoryService.getAllCategories();
	}
	
	private void getGCMRegisterId() {
		showProgressText(getResources().getString(R.string.progress_gcm));
		final Witherest application = (Witherest)getApplication();
		String regId = gcmController.registGCM();
		
		if (!CommonUtils.isNullOrEmpty(regId)){
			application.setGCMId(regId);
			application.setGCMReady(true);
			getCategories();
			
		}else {
			new Thread(){
				private boolean isGCMRegisterCompleted = false;
				public void run(){
					while(!isGCMRegisterCompleted){
						if (application.isGCMReady()){
							isGCMRegisterCompleted = true;
						}else {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {}
						}
					}	
					
					runOnUiThread(new Runnable(){
						public void run() {
							getCategories();
						}	
					});
				}
			}.start();
		}
	}
	
	public void checkAutoLogin() {
		WitherestPreference pref = new WitherestPreference(this);
		mUserId = pref.getString("email", null);
		mPassword = pref.getString("password", null);
		
		if (!CommonUtils.isNullOrEmpty(mUserId) && !CommonUtils.isNullOrEmpty(mPassword)) {
			showProgressText(getResources().getString(R.string.progress_autologin));
			ServiceManager manager = ServiceManager.getServiceManager();
			UserService userService = manager.getUserService();
			userService.setOnUserCallback(this);
			userService.login(mUserId, mPassword , ((Witherest)getApplication()).getGCMId());
		} else {
			progress.dismiss();
			mAnim.cancel();
			finish();
			
			Intent i = new Intent(this, ActivityMain.class);
			startActivity(i);
			
		}
	}
	
	public void onVersionServiceCallback(BaseResponseObject object) {
		if (object.resultCode == Service.RESULT_FAIL) {
			AndroUtils.showToastMessage(this, object.requestType + " : " + object.resultMsg, Toast.LENGTH_SHORT);
			progress.dismiss();
			mAnim.cancel();
				return;
			}
		
		if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
				object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.fatal_network_error);
			builder.setMessage(R.string.fatal_network_error_message);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.confirm,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							progress.dismiss();
							mAnim.cancel();
							finishDelayed();
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
									checkVersion();
								}
							}, 100);
						}
					});

			builder2.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							progress.dismiss();
							mAnim.cancel();
							finishDelayed();
						}
					});

			builder2.create().show();
			return;
		}
		
		VersionResponseObject obj = (VersionResponseObject) object;
		AppInfo appInfo = AppInfo.getInstance();
		
		if (appInfo.getVersion().getMajorVersion() < obj.getVersion().getMajorVersion()) {
			Log.v("version",appInfo.getVersion().getMajorVersion() +" : "+obj.getVersion().getMajorVersion());
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("업그레이드 안내");
			builder.setMessage("클라이언트가 최신버젼이 아닙니다.\n업그레이드를 하셔야 이용이 가능합니다");
			builder.setCancelable(false);
			builder.setPositiveButton("예",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							//AndroUtils.closeProgress(SplashActivity.this, progressBar);
							progress.dismiss();
							mAnim.cancel();
							finishDelayed();
							
						}
					});
			builder.setNegativeButton("아니요",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							progress.dismiss();
							mAnim.cancel();
							finish();
						}
					});

			builder.create().show();
		}
		
		else if (appInfo.getVersion().getMinorVersion() < obj.getVersion().getMinorVersion()) {
			appInfo.needUpgrade = true;
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle("업그레이드 안내");
			builder2.setMessage("클라이언트가 최신버젼이 아닙니다.\n업그레이드 하시겠습니까?\n업그레이드 하지 않으셔도 이용이 가능합니다.");
			builder2.setCancelable(false);
			builder2.setPositiveButton("예",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							progress.dismiss();
							mAnim.cancel();
							finishDelayed();
						}
					});
			builder2.setNegativeButton("아니요",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							getGCMRegisterId();
						}
					});
			builder2.create().show();
		}else {
			getGCMRegisterId();
		}
	}



	public void onCategoryServiceCallback(BaseResponseObject object) {
		if (object.resultCode == Service.RESULT_FAIL) {
			AndroUtils.showToastMessage(this, object.requestType + " : " + object.resultMsg, Toast.LENGTH_SHORT);
			progress.dismiss();
			mAnim.cancel();
			return;
			}
		
		if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
				object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.fatal_network_error);
			builder.setMessage(R.string.fatal_network_error_message);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.confirm,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							progress.dismiss();
							mAnim.cancel();
							finishDelayed();
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
									getCategories();
								}
							}, 100);
						}
					});

			builder2.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							progress.dismiss();
							mAnim.cancel();
							finishDelayed();
						}
					});

			builder2.create().show();
			return;
		}
		
		CategoryResponseObject obj = (CategoryResponseObject)object;
		AppCache cache  = AppCache.getInstance();
		cache.setAppCategory(obj.categories);
		checkAutoLogin();
	}
	
	public void onUserServiceCallback(BaseResponseObject object) {
		if (object.resultCode == Service.RESULT_FAIL) {
			AndroUtils.showToastMessage(this, object.resultMsg,
					Toast.LENGTH_LONG);
			progress.dismiss();
			mAnim.cancel();
			return;
		}
		
		switch (object.requestType) {
		case Service.REQUEST_TYPE_LOGIN:
			
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.fatal_network_error);
				builder.setMessage(R.string.fatal_network_error_message);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								progress.dismiss();
								mAnim.cancel();
								finishDelayed();
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
										checkAutoLogin();
									}
								}, 100);
							}
						});

				builder2.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								progress.dismiss();
								mAnim.cancel();
								finishDelayed();
							}
						});

				builder2.create().show();
				return;
			}
			
			LoginResponseObject obj = (LoginResponseObject) object;

			Session.getInstance().sessionStatus = Session.AUTHORIZED;
			Session.getInstance().sessionKey = obj.token;
			
			Session.getInstance().user = obj.user;
			Session.getInstance().user.id = mUserId;
			Session.getInstance().user.gcmId = ((Witherest)getApplication()).getGCMId();
			Session.getInstance().user.password = mPassword;
			
			progress.dismiss();
			mAnim.cancel();
			
			finish();
			 Intent i = new Intent(this, ActivityMain.class);
			startActivity(i);
			
		/*	final Intent i = new Intent(this, MainActivity.class);
			new Handler().postDelayed(new Runnable() {
				public void run() {
					// i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
					//AndroUtils.closeProgress(this, progressBar);
				}
			}, 200);*/
			
			break;
			
		case Service.REQUEST_TYPE_DUPL_CHECK:
			break;
			
		case Service.REQUEST_TYPE_JOIN:
			break;
			
		case Service.REQUEST_TYPE_LOGOUT:
			break;
		}
	}
	
	public void finishDelayed(){
		new Handler().postDelayed(new Runnable() {
			public void run() {
				finish();
			}
		}, 100);
	}
}
