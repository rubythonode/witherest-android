package com.teamteam.witherest;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.teamteam.customComponent.PageInfo;
import com.teamteam.customComponent.ScrollRefreshPager;
import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.customComponent.ScrollRefreshPager.OnScrollRefreshListener;
import com.teamteam.customComponent.popup.ActionItem;
import com.teamteam.customComponent.popup.QuickAction;
import com.teamteam.customComponent.view.RefreshListView;

import com.teamteam.witherest.FragmentRoomBoard.RoomBoardAdaper;
import com.teamteam.witherest.FragmentRoomBoard.RoomBoardViewHolder;
import com.teamteam.witherest.cacheload.ImageLoader;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.common.CommonUtils;
import com.teamteam.witherest.model.Session;

import com.teamteam.witherest.service.callback.ArticleServiceCallback;
import com.teamteam.witherest.service.callback.object.ArticleActionResponseObject;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.callback.object.ReplyListResponseObject;
import com.teamteam.witherest.service.callback.object.RoomBoardResponseObject;
import com.teamteam.witherest.service.callback.object.RoomBoardResponseObject.Message;
import com.teamteam.witherest.service.internal.ArticleService;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;

public class ActivityBoardContent extends Activity implements ArticleServiceCallback{
	
	public int mMessageId;
	public int mWriterId;
	public String mWriterImagePath;
	public String mWriterNickname;
	public String message;
	public String mWriteTime;
	public int mReplyCount;
	
	public int mRoomId;
	public int mUserType;
	
	public int mPage;
	public int mTotalRecordCount;
	public static final int PAGING_SIZE  = 10;
	
	public ImageView writerImageView;
	public TextView mWriterNameView;
	public TextView mMessageView;
	public TextView mReplyCountView;
	public TextView mWriteTimeView;
	
	public Button mSubmitBtn;
	public EditText mWriteEdit;
	
	public SimpleProgressDialog waitProgressDialog;
	public ImageLoader imageLoader; 
	public ReplyBoardAdapter replyBoardAdapter;
	public RefreshListView replyListView;
	
	public int mReturningActionCount= 0;
	
	public ArticleService articleService;
	public ReplyListResponseObject replyResponseObject;
	
	public ActionItem modifyItem;;
	public ActionItem deleteItem;
	
	public int mSelectionPosition;
	
	public ReplyBoardAdapter replyAdapter;
	public ScrollRefreshPager pager;
	public PageInfo pageInfo;
	
	/*자신의 글을 롱클릭한 후, 삭제를 진행하였으나, 네트워크 사정으로 제대로 수행되지 않았을
	때 , 사용자가 재시도 버튼을 눌렀을 때 다시 해당 글을 삭제하기 위하여 임시적으로 글 위치를 저장해두는 변수*/
	public int deletePositon = -1;
	
	public static final int ACTION_ID_MODIFY =1;
	public static final int ACTION_ID_DELETE =2;
	
	public static final String TAG = "BoardContentActivity";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.activity_board_content);
	    
	    initInstance();
	    initView();
		initListener();
	}
	
	private void initInstance() {
		Intent i = getIntent();
		if (i == null){
			AndroUtils.showToastMessage(this, R.string.wrong_approach, Toast.LENGTH_SHORT);
			return; 
		}
		
		mMessageId = i.getIntExtra("messageId", -1);
		mWriterId = i.getIntExtra("writerId", -1);
		mWriterImagePath = i.getStringExtra("writerImagePath");
		mWriterNickname = i.getStringExtra("writerNickname");
		message = i.getStringExtra("message");
		mWriteTime = i.getStringExtra("writeTime");
		mReplyCount = i.getIntExtra("replyCount",-1);
		mUserType = i.getIntExtra("userType",-1);
		mRoomId = i.getIntExtra("roomId", -1);
		Log.v("유저 타입", mUserType + "   !!!");
		imageLoader = new ImageLoader(this);
		
		
	
		articleService = ServiceManager.getServiceManager().getArticleService();
		setListClickActionItem();
	}
		
	private void initView() {
		replyListView = (RefreshListView)findViewById(R.id.reply_listview);
		writerImageView = (ImageView)findViewById(R.id.roomboard_row_image);
		mWriterNameView = (TextView)findViewById(R.id.roomboard_textview1);
		mMessageView = (TextView)findViewById(R.id.roomboard_textview2);
		mReplyCountView = (TextView)findViewById(R.id.reply_count);
		mWriteTimeView = (TextView)findViewById(R.id.roomboard_textview3);
		mSubmitBtn = (Button)findViewById(R.id.roomboard_write_btn);
		mWriteEdit = (EditText)findViewById(R.id.roomboard_write_edittext);
		
		String url = null;
		if (!CommonUtils.isNullOrEmpty(mWriterImagePath) && !"0".equals(mWriterImagePath)){
			url = Service.BASE_URL + mWriterImagePath;
		}
		imageLoader.displayImage(url, writerImageView,ImageLoader.DEFAULT_PROFILE_IMAGE);
	
		
		mWriterNameView.setText(mWriterNickname);
		mMessageView.setText(message);
		mWriteTimeView.setText(mWriteTime);
		mReplyCountView.setText(CommonUtils.int2string(mReplyCount));
		
		/**댓글 보기에서는 한번에 다 가져오니 페이징 기능을 끈다.*/
		pager = new ScrollRefreshPager(this, replyListView);
		pager.setPagingEnabled(false);
		mPage = 0;
	}

	private void initListener() {
		mSubmitBtn.setOnClickListener(submitBtnListener) ;
		pager.setRefreshListerner(refreshListener);
	}
	
	OnScrollRefreshListener refreshListener = new OnScrollRefreshListener() {
		public void onScrollRefresh() {
			pager.pagerActionType = ScrollRefreshPager.PAGER_ACTION_REFRESH;
			getReplysById(mMessageId);
			
		}
	};
	
	//댓글 쓰기 리스너 
	OnClickListener submitBtnListener = new View.OnClickListener() {
		public void onClick(View v) {
			if (CommonUtils.isNullOrEmpty(mWriteEdit.getText().toString().trim())){
				AndroUtils.showToastMessage(ActivityBoardContent.this, getResources().getString(R.string.no_text),
						Toast.LENGTH_SHORT);
				return;
			}
			checkForSubmit();
		}

	};
	
	public void checkForSubmit(){
		AndroUtils.hideSoftKeyboard(ActivityBoardContent.this, mWriteEdit);
		if (CommonUtils.isNullOrEmpty(mWriteEdit.getText().toString().trim())){
			AndroUtils.showToastMessage(ActivityBoardContent.this,getResources().getString(R.string.no_text),
					Toast.LENGTH_SHORT);
			return;
		}
		
		if (mUserType == ActivityRoomDetail.MEMBER_JOINED || mUserType ==ActivityRoomDetail.OWNER){
			submitReply(mWriteEdit.getText().toString().trim());
		}else {
			AndroUtils.showToastMessage(ActivityBoardContent.this, getResources().getString(R.string.not_user_not_join_alert), 
					Toast.LENGTH_SHORT);
			return;
		}
	}
	
	private void setListClickActionItem() {
		
		modifyItem	= new ActionItem(ACTION_ID_MODIFY, "수정 ", getResources().getDrawable(R.drawable.comment_comment_icon_bg));
		deleteItem 	= new ActionItem(ACTION_ID_DELETE , "삭제", getResources().getDrawable(R.drawable.comment_trash_icon_bg));
    	
	}
        
	
	private void submitReply(String string) {
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		
		ArticleService articleService = ServiceManager.getServiceManager().getArticleService();
		articleService.setOnArticleCallback(this);	
		articleService.submitReplyComment(mWriteEdit.getText().toString().trim(), 
				ActivityRoomDetail.mRoomWithFragment.roomWithResponse.checkRoomWith.roomId, mMessageId);
		
	}

	public void onArticleServiceCallback(BaseResponseObject object) {
		if (waitProgressDialog.isShowing()){
			waitProgressDialog.dismiss();
		}
		
		if (object.resultCode == Service.RESULT_FAIL) {
			/*if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_REFRESH){
				pager.onRefreshingComplete(false, 0);
			}else if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_MORE){
				pager.onRefreshingComplete(false, 0);
			}else if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_DEFAULT){
				return;
			}*/
			
			return;
		}
		
		
		switch(object.requestType){
		case Service.REQUEST_TYPE_SUBMIT_REPLY_COMMENT :
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
										checkForSubmit();
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
			ArticleActionResponseObject obj = (ArticleActionResponseObject)object;
			addReplyToList(obj);
			break;
			
		case Service.REQUEST_TYPE_DELETE_COMMENT:
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
										if (deletePositon != -1)
											deleteMyComment(replyResponseObject.messageList.get(deletePositon));
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
			if (deletePositon == -1)
				deletePositon = -1;
			
			ArticleActionResponseObject obj1 = (ArticleActionResponseObject)object;
			deleteReplyFromList(obj1);
			break;
			
		case Service.REQUEST_TYPE_GET_REPLYS_BY_ID:
			
			if (object.resultCode == ConnectionErrorHandler.COMMON_ERROR || object.resultCode == ConnectionErrorHandler.NETWORK_DISABLE ||
					object.resultCode == ConnectionErrorHandler.PARSING_ERROR){
				
				if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_REFRESH){
					pager.onRefreshingComplete(false, replyResponseObject != null ? replyResponseObject.totalBoardCount : 0);
				}else if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_MORE){
					pager.onRefreshingComplete(false, 0);
				}
				
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
										getReplysById(mMessageId);
									}
								}, 100);
							}
						});

				builder2.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_REFRESH){
									pager.onRefreshingComplete(false, replyResponseObject != null ? replyResponseObject.totalBoardCount : 0);
								}else if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_MORE){
									pager.onRefreshingComplete(false, 0);
								}
							}
						});

				builder2.create().show();
				return;
			}
			
			ReplyListResponseObject obj2 = (ReplyListResponseObject)object;
			if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_REFRESH ){
				pager.onRefreshingComplete(true,obj2.totalBoardCount);
				updateUi(obj2);
			
			}else if(pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_MORE){
				pager.onPagingComplete(true, obj2.totalBoardCount);
				addItemToList(obj2);
			
			}else if (pager.pagerActionType == ScrollRefreshPager.PAGER_ACTION_NON){
				updateUi(obj2);
			}

			break;
		}			
	}
	 
	
	private void updateUi(ReplyListResponseObject object) {
		replyResponseObject = object;
		pageInfo = new PageInfo(mRoomId,mPage,object.totalBoardCount );
		replyAdapter = new ReplyBoardAdapter(this,imageLoader, R.layout.roomboard_list_item_row, 
				object.messageList);
		pager.setPageInfo(pageInfo);
		pager.setData(object.messageList);
		replyBoardAdapter = new ReplyBoardAdapter(this, imageLoader, R.layout.roomboard_list_item_row,
				replyResponseObject.messageList);
		setAdapter(replyAdapter);
	}

	
	private void setAdapter(ReplyBoardAdapter adpater) {
		pager.setAdapter(replyBoardAdapter);
	}

	
	private void addItemToList(ReplyListResponseObject replyObject) {
		this.mPage = pager.getCurrentPage();

		ArrayList<Message> originalMessageList = replyResponseObject.messageList;
		ArrayList<Message> messageList = replyObject.messageList;
		
		for (int i = 0; i < messageList.size(); i++){
			Message message = messageList.get(i);
			originalMessageList.add(message);
		}
		pager.pagerActionType = ScrollRefreshPager.PAGER_ACTION_NON;
		pager.notifyDataSetChanged();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		waitProgressDialog = new SimpleProgressDialog(ActivityBoardContent.this, 
				getString(R.string.wait_title),getString(R.string.wait_message));
		waitProgressDialog.start();
		
		Log.v(TAG,"onResume() 호출");
		if (replyResponseObject == null){
			
			pager.pagerActionType = ScrollRefreshPager.PAGER_ACTION_NON;
			getReplysById(mMessageId);
		}
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		SimpleProgressDialog.end(waitProgressDialog);
		mSelectionPosition = replyListView.getFirstVisiblePosition();
		Log.v(TAG,"onPause() 호출");
		Log.v(TAG,"포지션 ==>" + mSelectionPosition);
	}

	private void getReplysById(int messageId) {
		if (replyResponseObject == null){
			if (!waitProgressDialog.isShowing()){
				waitProgressDialog.show();
			}
		}
	
		articleService.setOnArticleCallback(this);
		articleService.getReplysById(mRoomId, messageId);	
	}
	
	public int findPositionById(int id ){
		ArrayList<Message> messageList = replyResponseObject.messageList;
		int index = -1;
		
		for (int i = 0; i < messageList.size(); i++){
			Message message = messageList.get(i);
			if (message.messageId == id){
				index = i;
				break;
			}
		}
		return index;
	}
	
	private void refreshList() {
		replyBoardAdapter.notifyDataSetChanged();
	}

	private void addReplyToList(ArticleActionResponseObject obj) {
		mReturningActionCount++;
		Log.v("returningActionCoun",mReturningActionCount + "!!!");
		Message message = new Message();
		message.messageId = obj.messageId;
		message.writeId = Session.getInstance().user.userIndex;
		message.writerNickname = Session.getInstance().user.nickName;
		message.writeTime= getResources().getString(R.string.just);
		message.isReply = true;
		message.article_type = 0;
		message.replyCount = 0;
		message.message =  mWriteEdit.getText().toString().trim();
		message.parentId = 0;
		message.writerImagePath = Session.getInstance().user.profileImagePath;
		
		replyResponseObject.messageList.add(0,message);
		resetEdit();
		refreshList();
		replyListView.setSelection(0);
	}
	
	private void deleteReplyFromList(ArticleActionResponseObject obj1) {
		mReturningActionCount--;
		int index = findPositionById(obj1.messageId);
		replyResponseObject.messageList.remove(index);
		refreshList();
		replyListView.setSelection(mSelectionPosition);
	}
	
	private void resetEdit() {
		 mWriteEdit.setText("");
	}

	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent();
			i.putExtra("parentMessageId", mMessageId);
			i.putExtra("increase", mReturningActionCount);
			setResult(RESULT_OK, i);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void deleteMyComment(Message message) {
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		
		mSelectionPosition = pager.getSelection();
		
		ArticleService articleService = ServiceManager.getServiceManager().getArticleService();
		articleService.setOnArticleCallback(this);	
		articleService.deleteComment(message.messageId);
	}
	
	
	public class ReplyBoardAdapter extends BaseAdapter{
		
		Context con;
		int resId;
		ArrayList<Message> messageList;
		LayoutInflater inflater;
		ImageLoader imageLoader;
		
		public ReplyBoardAdapter(Context con, ImageLoader imageLoader, int resId, ArrayList<Message> messageList2){
			this.con = con;
			this.imageLoader = imageLoader;
			this.resId = resId;
			this.messageList = messageList2;
			this.inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public int getCount() {
			return messageList.size();
		}
		
		public Object getItem(int position) {
			return messageList.get(position);
		}
		
		public long getItemId(int position) {
			return position;
		}
		public View getView(int position, View convertView, ViewGroup parent) {
			final int  pos = position;
			ReplyViewHolder viewHolder = null; 
			if (convertView == null){
				convertView = inflater.inflate(resId, parent, false);
				viewHolder = new ReplyViewHolder();
				viewHolder.writerImageView = (ImageView) convertView.findViewById(R.id.roomboard_row_image);
				viewHolder.wirterNameView =   (TextView)convertView.findViewById(R.id.roomboard_textview1);
				viewHolder.messageView =  (TextView)convertView.findViewById(R.id.roomboard_textview2);
				viewHolder.replyCoutView = (TextView)convertView.findViewById(R.id.reply_count);
				viewHolder.writeTimeView = (TextView)convertView.findViewById(R.id.roomboard_textview3);
				viewHolder.replyContainer = convertView.findViewById(R.id.reply_container);
				viewHolder.lineSeparator = convertView.findViewById(R.id.line_separator);
				convertView.setTag(viewHolder);
			}else {
				viewHolder = (ReplyViewHolder )convertView.getTag();
			}
			viewHolder.lineSeparator.setVisibility(View.INVISIBLE);
			viewHolder.writerImageView.setImageBitmap(null);
			viewHolder.replyContainer.setVisibility(View.GONE);
			
			String url = null;
			if (!CommonUtils.isNullOrEmpty(messageList.get(position).writerImagePath) &&
					!"0".equals(messageList.get(position).writerImagePath)){
				 url = Service.BASE_URL+messageList.get(position).writerImagePath;
			}
			
			imageLoader.displayImage(url, viewHolder.writerImageView, ImageLoader.DEFAULT_PROFILE_IMAGE); 
			viewHolder.wirterNameView.setText(messageList.get(position).writerNickname);
			viewHolder.messageView.setText(messageList.get(position).message);
			viewHolder.writeTimeView.setText(messageList.get(position).writeTime);
			final View  container = convertView.findViewById(R.id.roomboard_contaniner);
			final QuickAction listQuickAction = new QuickAction(con, QuickAction.VERTICAL);
			final TextView anchorView = viewHolder.messageView;
			
			listQuickAction.addActionItem(deleteItem);
			container.setOnLongClickListener(new OnLongClickListener() {
				public boolean onLongClick(View v) {
					if (Session.getInstance().sessionStatus != Session.AUTHORIZED){
						return true;
					}
					if (Session.getInstance().user.userIndex !=messageList.get(pos).writeId){
						return true;
					}
				
					listQuickAction.show(anchorView);
					return true;
					}
				});
		    listQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
				public void onItemClick(QuickAction source, int actionPos, int actionId) {				
					ActionItem actionItem = listQuickAction.getActionItem(actionPos);
					if (actionId == ACTION_ID_MODIFY) {
							
					} else if (actionId == ACTION_ID_DELETE) {
						deletePositon = pos;
						deleteMyComment(messageList.get(pos));
				    }
				}
	        });		
			return convertView;
		}
	}
	
	public class ReplyViewHolder{
		public View lineSeparator;
		public View replyContainer;
		public ImageView writerImageView;
		public TextView wirterNameView;
		public TextView messageView;
		public TextView replyCoutView;
		public TextView writeTimeView;
	}
}
