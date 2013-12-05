package com.teamteam.witherest;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.cacheload.ImageLoader;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.common.CommonUtils;
import com.teamteam.witherest.service.callback.CategoryServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.callback.object.CategoryRoomListResponseObject;
import com.teamteam.witherest.service.callback.object.MyCheckResponseObject.CheckRoom;
import com.teamteam.witherest.service.internal.CategoryService;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;

public class FragmentCheckRoomList extends ListFragment{
	
	
	public String  mTitle;
	public int mListType;;
	
	public int mCategoryId;
	public int mPage;
	public int mCurRecordCount;
	public int mTotalRecordCount;
	public static final int PAGING_SIZE  = 10;
	
	public Activity mAct;
	public ImageLoader imageLoader;
	public CategoryRoomListResponseObject mRoomListResponse;
	public View mFragmentView;
	public CheckRoomListFragmentAdapter mCheckRoomListFragmentAdapter;
	
	public View mFooter;
	public ProgressBar mLoadFooter;
	public boolean mIsListLoading = true;
	public boolean mIsFooterExisted = false;
	public AnimationDrawable mFooterSpinAnim;
	
	public CategoryService categoryService = ServiceManager.getServiceManager().getCategoryService();
	
	public static final String TAG ="CheckRoomListFragment ";
	
	public static FragmentCheckRoomList  newInstance(String title, int page,int categoryId, int listType){
		FragmentCheckRoomList fragment = new FragmentCheckRoomList ();
		fragment.setInitalData(title, page, categoryId, listType);
	    Bundle args = new Bundle();
	    args.putString("title", title);
        args.putInt("listType", listType);
        args.putInt("page", page);
        args.putInt("categoryId", categoryId);
        fragment.setArguments(args);
        return fragment;
	}
	
    @Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		this.mAct = activity;

	     /*  프래그먼트 각각이 ImageLoader 를 생성해서 별도의 캐시를 관리하고자 하면 
	     아래의 코드의 주석을 푼다 
	     imageLoader = new ImageLoader(getActivity().getApplicationContext());*/
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		mFragmentView= inflater.inflate(R.layout.fragment_room_list, null);
		mFooter = inflater.inflate(R.layout.footer, null, false);
        mLoadFooter = (ProgressBar) mFooter.findViewById(R.id.iv_list_footer_loading);
        //footerSpinAnim = (AnimationDrawable) loadFooter.getBackground();
		return mFragmentView;	
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
		super.onResume();	
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	public void setInitalData(String title, int page, int categoryId,int listType){
		this.mTitle = title;
		this.mPage = page;
		this.mCategoryId = categoryId;
		this.mListType = listType;
	}
	
	public void setImageLoader(ImageLoader imageLoader){
		this.imageLoader = imageLoader;
	}
	
	public void setRoomListObject(CategoryRoomListResponseObject object, int listType){
		this.mRoomListResponse = object;
		this.mTotalRecordCount = object.totalRoomCount;
		updateUi();
	}

	public void updateUi(){
		if (this.mTotalRecordCount > getCurRecordCount()){
			getListView().addFooterView(mFooter);
			mIsFooterExisted = true;
			
		}
		mCheckRoomListFragmentAdapter= new CheckRoomListFragmentAdapter(getActivity(),
				R.layout.fragment_room_list_item, mRoomListResponse, imageLoader);	
		setListAdapter(mCheckRoomListFragmentAdapter);	
		getListView().setOnScrollListener(scrollListener);
		mIsListLoading = false;
	}
	
	public int getCurRecordCount(){
		return mPage * PAGING_SIZE;
	}
	
	public void getCheckRooms() {
		categoryService.setOnCategoryCallback(categoryServiceCallback);
		
		//footerSpinAnim.start();
		mLoadFooter.setVisibility(View.VISIBLE);
		categoryService.getRoomsByCategory(mCategoryId,mPage+1,mListType);
	}
	
	CategoryServiceCallback categoryServiceCallback = new CategoryServiceCallback() {
		public void onCategoryServiceCallback(BaseResponseObject object) {
			if (object.resultCode == Service.RESULT_FAIL) {
				mLoadFooter.setVisibility(View.INVISIBLE);
				mIsListLoading = false;
				return;
			}
			
			switch(object.requestType){
			case Service.REQUEST_TYPE_GET_ROOMS_BY_CATEGORY:
				
				if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
						object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
					
					mLoadFooter.setVisibility(View.GONE);
					
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
											getCheckRooms(); 
										}
									}, 100);
								}
							});

					builder2.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									mLoadFooter.setVisibility(View.GONE);
								}
							});

					builder2.create().show();
					return;
				}
				
				mPage++;
				CategoryRoomListResponseObject response = (CategoryRoomListResponseObject)object;
				addItemsToList(response);
				break;
			}
		}
	};
	
	public void addItemsToList(CategoryRoomListResponseObject object){
		this.mTotalRecordCount = object.totalRoomCount;
		if (this.mTotalRecordCount <= getCurRecordCount() || object.roomList.size()< PAGING_SIZE){
			getListView().removeFooterView(mFooter);
			mIsFooterExisted = false;
			getListView().setOnScrollListener(null);
		}
		
		if (object.roomList.size()>0){
			for (int i = 0; i < object.roomList.size() ; i++ ){
				mRoomListResponse.roomList.add(object.roomList.get(i));
			}
			this.mCheckRoomListFragmentAdapter.notifyDataSetChanged();
		}
		mLoadFooter.setVisibility(View.INVISIBLE);
		mIsListLoading = false;
	}
	
	OnScrollListener scrollListener = new OnScrollListener() {
		public void onScrollStateChanged(AbsListView view, int scrollState) {	}
		
		public void onScroll(AbsListView view, int firstVisibleItem,int visibleItemCount, int totalItemCount) {
			if ((firstVisibleItem + visibleItemCount) == totalItemCount) { // /
                if (mIsFooterExisted){
					if (!mIsListLoading) {
	                        mIsListLoading = true;
	                        getCheckRooms();
	                }
                }
			} 
		}
	};
	
	public static class CheckRoomListFragmentAdapter extends BaseAdapter{
		public Context  context ; 
		public int resId ;
		public LayoutInflater inflate;
		public CategoryRoomListResponseObject roomListRespose;
		public ImageLoader imageLoader; 
	
		public CheckRoomListFragmentAdapter (Activity act, int layoutRes,CategoryRoomListResponseObject roomListRespose,
				ImageLoader imageLoader){
			
			this.context = act;
			this.resId = layoutRes;
			this.roomListRespose = roomListRespose;
		
			this.inflate = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			this.imageLoader=imageLoader;
		}
		
		public int getCount() {
			return roomListRespose.roomList.size();
		}

		public Object getItem(int position) {
			return roomListRespose.roomList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final int pos = position;
			RoomListViewHolder viewHolder ; 
			
			if (convertView == null){
				convertView = inflate.inflate(resId,parent, false);
				viewHolder = new RoomListViewHolder();
				viewHolder.roomImage = (ImageView)convertView.findViewById(R.id.room_list_image);
				viewHolder.roomTitle = (TextView)convertView.findViewById(R.id.room_list_title_textview);
				viewHolder.roomPurpose =  (TextView)convertView.findViewById(R.id.room_list_purpose_textview);
				viewHolder.roomTerm =  (TextView)convertView.findViewById(R.id.room_list_term_textview);
				viewHolder.roomJoinState = (TextView)convertView.findViewById(R.id.room_list_joincount_textview);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (RoomListViewHolder)convertView.getTag();
			}
			
			/*Log.v( title + " 리스트 포지션" + position, roomListRespose.roomList.get(position).roomTitle+"");*/
			
			viewHolder.roomImage.setImageBitmap(null);
			String url = null;
			if ( CommonUtils.isNullOrEmpty(roomListRespose.roomList.get(position).roomImagePath) || 
					"0".equals(roomListRespose.roomList.get(position).roomImagePath)){	
			}else if( !CommonUtils.isNullOrEmpty(roomListRespose.roomList.get(position).roomImagePath)) {
				 url = Service.BASE_URL+roomListRespose.roomList.get(position).roomImagePath;		
			}
			 imageLoader.displayImage(url, viewHolder.roomImage, ImageLoader.DEFAULT_ROOM_IMAGE);
		
			viewHolder.roomTitle.setText(roomListRespose.roomList.get(position).roomTitle);
			viewHolder.roomPurpose.setText(roomListRespose.roomList.get(position).roomPurpose);
			viewHolder.roomTerm.setText(roomListRespose.roomList.get(position).startDate + " ~ " + roomListRespose.roomList.get(position).endDate);
			viewHolder.roomJoinState.setText(roomListRespose.roomList.get(position).curMemberCount + " / " 
					+ roomListRespose.roomList.get(position).maxMemberCount );
			
			convertView.findViewById(R.id.room_list_contaniner).setOnClickListener(new View.OnClickListener() {
				public void onClick(View arg0) {
					Intent i = new Intent(context, ActivityRoomDetail.class);
					i.putExtra("roomId",roomListRespose.roomList.get(pos).roomId );
					i.putExtra("roomTitle", roomListRespose.roomList.get(pos).roomTitle);
					context.startActivity(i);	
				}
			});
			return convertView;
		}
	}
	
	public static class RoomListViewHolder{
		public ImageView roomImage;
		public TextView roomTitle;
		public TextView roomPurpose ;
		public TextView roomTerm; 
		public TextView roomJoinState;
	}
}
