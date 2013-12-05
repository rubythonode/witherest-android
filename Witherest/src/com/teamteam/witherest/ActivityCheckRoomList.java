package com.teamteam.witherest;



import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;
import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.cacheload.ImageLoader;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.service.callback.CategoryServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;

import com.teamteam.witherest.service.callback.object.CategoryRoomListResponseObject;
import com.teamteam.witherest.service.internal.CategoryService;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TitlePageIndicator;


public class ActivityCheckRoomList extends FragmentActivity implements OnPageChangeListener, CategoryServiceCallback {
	   public static String TAG = "CheckRoomListActivity";
	
	   public static int DEFALUT_PAGE= 1;
	   
	   public static int ROOMLIST_ORDER_REGISTER_DATE = 1;
	   public static int ROOMLIST_ORDER_PUPULATE = 2;
	
	   public CheckRoomViewPagerAdapter mCheckRoomViewPagerAdapter;
	   public FragmentCheckRoomList[] roomListFragments  = new FragmentCheckRoomList[2];
	   public ViewPager mPager;
	   public PageIndicator mIndicator;
	   public SimpleProgressDialog waitProgressDialog;
	   
	   public CategoryRoomListResponseObject regRoomListObject;
	   public CategoryRoomListResponseObject popRoomListObject;
	  
	   public CategoryService categoryService;
	   public ImageLoader imageLoader;
	   
	   public int mCategoryId;
	   public int mRequestType; 
	   public boolean mIsAllLoaded = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.activity_roomlist);
	    initInstance();
	    initView(); 
	}
	
	public void initInstance(){
		imageLoader = new ImageLoader(getApplicationContext());
		categoryService = ServiceManager.getServiceManager().getCategoryService();
		
		Intent i = getIntent();
        mCategoryId = i.getIntExtra("categoryId", -1);
        if (mCategoryId == -1){
        	AndroUtils.showToastMessage(this, R.string.inappropriate_path, Toast.LENGTH_SHORT);
        	return;
        }
	
		/*
		 * 현재 리스트프그래먼트에서 사용하는 ImageLoader 는 같은 객체를 사용하며, 캐시를 공유하는 식으로 작성하기 위해서 
		   같은 ImageLoader 를 아래와 같이 전달한다.
		   각각의 객체를 사용하는 것보다는 느리지만, 매모리를 덜 사용한다.
		   속도상의 문제가 있을 경우는 각각의 ImageLoader 객체를 할당하되, 적절하게 사용 캐시 메모리 용량을 지정한다.
		*/
		roomListFragments[0]= FragmentCheckRoomList.newInstance(getResources().getString(R.string.registration_order),DEFALUT_PAGE, mCategoryId,ROOMLIST_ORDER_REGISTER_DATE);
		roomListFragments[0].setImageLoader(imageLoader);
		
		roomListFragments[1] = FragmentCheckRoomList.newInstance(getResources().getString(R.string.popular_order), 
				DEFALUT_PAGE, mCategoryId,ROOMLIST_ORDER_PUPULATE);
		roomListFragments[1].setImageLoader(imageLoader);

	}
	
	public void initView(){
		mCheckRoomViewPagerAdapter = new CheckRoomViewPagerAdapter(getSupportFragmentManager(),roomListFragments);
		mPager = (ViewPager)findViewById(R.id.pager);
	    mPager.setAdapter(mCheckRoomViewPagerAdapter);

	    mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
	    mIndicator.setViewPager(mPager);
	    mIndicator.setOnPageChangeListener(this);
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		waitProgressDialog = new SimpleProgressDialog(this, getResources().getString(R.string.wait_title),
    			getString(R.string.wait_message));   
		waitProgressDialog.start();
		if (!mIsAllLoaded){
			   getCheckRooms(ROOMLIST_ORDER_REGISTER_DATE);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		SimpleProgressDialog.end(waitProgressDialog);
	}

	public void getCheckRooms(int type){
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		
		mRequestType = type;
		CategoryService categoryService = ServiceManager.getServiceManager().getCategoryService();
		categoryService.setOnCategoryCallback(this);
		categoryService.getRoomsByCategory(mCategoryId,DEFALUT_PAGE, type);
	}
	
	public void onPageScrollStateChanged(int state) {}
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
	public void onPageSelected(int position) {}

	public void onCategoryServiceCallback(BaseResponseObject object) {
		
		/*if (waitProgressDialog.isShowing()){
			SimpleProgressDialog.stop(waitProgressDialog);
		}*/
		
		if (object.resultCode == Service.RESULT_FAIL) {
			return;
		}
	
		switch(object.requestType){
		case Service.REQUEST_TYPE_GET_ROOMS_BY_CATEGORY:
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				if (waitProgressDialog.isShowing()){
					waitProgressDialog.dismiss();
				}
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
				if (waitProgressDialog.isShowing()){
					waitProgressDialog.dismiss();
				}
				
				AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
				builder2.setTitle(R.string.tempo_network_error);
				builder2.setMessage(R.string.tempo_network_error_message);
				builder2.setCancelable(false);
				builder2.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Handler().postDelayed(new Runnable() {
									public void run() {
										 getCheckRooms(ROOMLIST_ORDER_REGISTER_DATE);
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
			
			if ( mRequestType == ROOMLIST_ORDER_REGISTER_DATE ){
				regRoomListObject = (CategoryRoomListResponseObject)object;
				getCheckRooms(ROOMLIST_ORDER_PUPULATE );
				
			}else if (mRequestType == ROOMLIST_ORDER_PUPULATE ){
				if (waitProgressDialog.isShowing()){
					waitProgressDialog.dismiss();
				}
				popRoomListObject = (CategoryRoomListResponseObject)object;
				mIsAllLoaded = true;
			}
			break;
		}
		
		if (mIsAllLoaded){
			 Log.v( TAG ,"등록순 인기도순 데이타 수신 완료");
			setRoomListToFragments();
		}
	}

	private void setRoomListToFragments() {
		roomListFragments[0].setRoomListObject(regRoomListObject,ROOMLIST_ORDER_REGISTER_DATE);
		roomListFragments[1].setRoomListObject(popRoomListObject,ROOMLIST_ORDER_PUPULATE);	
	}

}