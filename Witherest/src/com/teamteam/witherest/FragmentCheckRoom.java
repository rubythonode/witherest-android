package com.teamteam.witherest;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.common.CommonUtils;
import com.teamteam.witherest.model.AppCache;
import com.teamteam.witherest.model.Category;
import com.teamteam.witherest.service.callback.CategoryServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.callback.object.CategoryRoomCountResponseObject;
import com.teamteam.witherest.service.callback.object.MyCheckResponseObject;
import com.teamteam.witherest.service.callback.object.CategoryRoomCountResponseObject.CategoryRoomCount;
import com.teamteam.witherest.service.internal.CategoryService;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;

public class FragmentCheckRoom extends Fragment implements CategoryServiceCallback{

	private Activity mActivity;
	private SimpleProgressDialog waitProgressDialog;
	public CategoryRoomCountResponseObject mRoomCountObject;
	public LinearLayout mCheckRoomView;

	/* Fragment Life Cycle */

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mActivity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		mCheckRoomView = (LinearLayout) inflater.inflate(R.layout.fragment_checkroom, null);
		return mCheckRoomView;
	}
	
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		waitProgressDialog = new SimpleProgressDialog(getActivity(), getActivity().getString(R.string.wait_title),
				getString(R.string.wait_message));
			waitProgressDialog.start();
			
		//카테고리별 방 정보는 매번 읽어오지 않게 끔 작성함
		if (mRoomCountObject == null ){
			loadAllCategoryRoomCount();
		}else {
			updateUi();
		}
	}


	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onResume();
		SimpleProgressDialog.end(waitProgressDialog);
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
	
	public void updateUi(){

		ArrayList<Category> categories = AppCache.getInstance().getAppCategory();

		ListView checkRoomList = (ListView) mCheckRoomView.findViewById(R.id.check_room_list);
		checkRoomList.setAdapter(new CategoryListAdapter(mActivity,R.layout.check_room_list_item, 
				categories,mRoomCountObject.allCategoriesRoomCount));
		checkRoomList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View view, int position,
					long id) {
				if (mRoomCountObject.allCategoriesRoomCount.get(position).roomCount >0){
					Intent i = new Intent(mActivity, ActivityCheckRoomList.class);
					i.putExtra("categoryId", AppCache.getInstance().getAppCategory().get(position).categoryId);
					mActivity.startActivity(i);
				}
			}
		});
	}
	
	public void loadAllCategoryRoomCount(){
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		CategoryService categoryService = ServiceManager.getServiceManager().getCategoryService();
		categoryService.setOnCategoryCallback(this);	
		categoryService.getAllCagetoryRoomCount();
	}
	
	public void onCategoryServiceCallback(BaseResponseObject object) {
		if (waitProgressDialog.isShowing()){
			waitProgressDialog.dismiss();
		}
		if (object.resultCode == Service.RESULT_FAIL) {
			return;
		}
		
		switch(object.requestType){
		case Service.REQUEST_TYPE_GET_ALL_CATEGORIES_ROOMCOUNT:
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
										loadAllCategoryRoomCount();
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
			mRoomCountObject = (CategoryRoomCountResponseObject)object;
			updateUi();
			break;
		}
		
	}
	
	class CategoryListAdapter extends BaseAdapter{
		LayoutInflater inflater;
		int layout; 
		ArrayList<Category> categories;
		ArrayList<CategoryRoomCount> categoryRoomCount;
		Context context;
		
		public CategoryListAdapter(Context context, int layout, ArrayList<Category> list,ArrayList<CategoryRoomCount> categoryRoomCount){
			this.context = context;
			this.layout = layout;
			this.categories = list;
			this.categoryRoomCount = categoryRoomCount;
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		public int getCount() {
			return categories.size();
		}

		public Object getItem(int position) {
			return categories.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final int pos = position;
			if (convertView == null){
				convertView = inflater.inflate(layout, parent, false);
			}
			TextView categoryTextView = (TextView) convertView.findViewById(R.id.check_room_textview);
			TextView  roomCountTextView = (TextView)convertView.findViewById(R.id.check_room_count_textview);
			
			categoryTextView .setText(categories.get(position).categoryName);
			roomCountTextView.setText(CommonUtils.int2string(categoryRoomCount.get( position).getRoomCount()));
			return convertView;
		}
	}

	/* /Fragment Life Cycle */
}