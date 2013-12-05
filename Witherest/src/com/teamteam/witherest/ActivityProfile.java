package com.teamteam.witherest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.teamteam.customComponent.SimpleProgressDialog;
import com.teamteam.witherest.ActivityMakeRoom.MakeRoomSettingDialogFragment;
import com.teamteam.witherest.cacheload.ImageLoader;
import com.teamteam.witherest.common.AndroUtils;
import com.teamteam.witherest.common.CommonUtils;
import com.teamteam.witherest.model.AppCache;
import com.teamteam.witherest.model.Category;
import com.teamteam.witherest.model.Session;
import com.teamteam.witherest.model.UserProfile;
import com.teamteam.witherest.service.callback.UserServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;
import com.teamteam.witherest.service.callback.object.CreateRoomResponseObject;
import com.teamteam.witherest.service.callback.object.RoomInfoResponseObject;
import com.teamteam.witherest.service.internal.ConnectionErrorHandler;
import com.teamteam.witherest.service.internal.Service;
import com.teamteam.witherest.service.internal.ServiceManager;
import com.teamteam.witherest.service.internal.UserService;

public class ActivityProfile extends  FragmentActivity  implements View.OnClickListener , UserServiceCallback{

	public ArrayList<Category> appCategories;
	
	public TextView mNickNameTextView;
	public TextView mEmailTextView;
	public TextView mPurposeTextView;
	public TextView mMyCategoryTextView;
	public ImageView mProfileImageView;
	
	public String mProfileImagePath;
	public String mName;
	public String mEmail;
	public String mPurpose;
	public String mCategories;
	public ArrayList<Category> mCategoryList;
	
	private SimpleProgressDialog waitProgressDialog;
	public UserService userService;
	public ImageLoader imageLoader; 
	
	//방 이미지 변경을 위한 변수로, 갤러리, 카메라, 크랍인텐트를 나타냄
	public static final int PICK_FROM_ALBUM = 0;
	public static final int PICK_FROM_CAMERA = 1;
	public static final int CROP_PICK_FROM_ALBUM = 2;
	public static final int CROP_FROM_IMAGE = 3;
	
	//다이알로그 생성 변수 
	public static final int DIALOG_PROFILE_IMAGE= 0;    
	public static final int DIALOG_NICNAME= 1;   
	public static final int DIALOG_EMAIL= 2;   
	public static final int DIALOG_PURPOSE= 3;   
	public static final int DIALOG_MYCATEGOTY= 4;   
	
	public String mTmpFile ;
	public String mTmpImagePath ;
	public static Uri mImageCaptureUri;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_profile);
		
		initInstance();
		initView();
		initListener();
	}

	private void initInstance() {
		appCategories = AppCache.getInstance().getAppCategory();
		userService = ServiceManager.getServiceManager().getUserService();
		imageLoader=new ImageLoader(getApplicationContext());
		//imageLoader.clearCache();
		
		mCategoryList = new ArrayList<Category>();
		
		mName = Session.getInstance().user.nickName;
		mEmail = Session.getInstance().user.id;
		mPurpose = Session.getInstance().user.purpose;
		mProfileImagePath = Session.getInstance().user.profileImagePath;
		clonCategoies(Session.getInstance().user.categories);
		
	}

	private void clonCategoies(ArrayList<Category> categories) {
		for (Category cat : categories){
			Category category = new Category(cat.categoryId,cat.categoryName,cat.categoryDescription);
			mCategoryList.add(category);
		}
	}

	private void initView() {
		mProfileImageView = (ImageView)findViewById(R.id.activity_profile_thumb_image);
	
		mNickNameTextView = (TextView)findViewById(R.id.activity_profile_nickname);
		mEmailTextView= (TextView)findViewById(R.id.activity_profile_email);
		mPurposeTextView = (TextView)findViewById(R.id.activity_profile_purpose);
		mMyCategoryTextView = (TextView)findViewById(R.id.activity_profile_mycatefories);
		
		if (mName != null){
			mNickNameTextView.setText(mName);
		}
		
		if (CommonUtils.isNullOrEmpty(mPurpose)){
			mPurposeTextView.setText(mPurpose);
		}
		
		mMyCategoryTextView.setText(getCategorieStr(mCategoryList));
		mEmailTextView.setText(mEmail);
		
		Log.v("유저 이름", mName+ "  ---");
		Log.v("유저 이메일", mEmail+ "  ---");
		Log.v("유저 목표문구", mPurpose+ "  ---");
		Log.v("유저 이미지 패스", mProfileImagePath+ "  ---");
		Log.v("유저 카테고리", getCategorieStr(mCategoryList)+ "  ---");
		
		//세션에 프로파일 이미지 패스가 있다면 다운로드 한다.
		String url = null;
		if ( !CommonUtils.isNullOrEmpty(mProfileImagePath)){
			url = Service.BASE_URL + mProfileImagePath;
			Log.v("이미지 다운 경로", url);
		}
		imageLoader.displayImage(url, mProfileImageView, ImageLoader.DEFAULT_PROFILE_IMAGE);
	}

	
	
	@Override
	protected void onPause() {// TODO Auto-generated method stub
		super.onPause();
		SimpleProgressDialog.end(waitProgressDialog);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		waitProgressDialog = new SimpleProgressDialog(ActivityProfile.this, getString(R.string.wait_title),
				getString(R.string.wait_message));
		waitProgressDialog.start();
	}

	private String getCategorieStr(ArrayList<Category> categories) {
		StringBuffer buffer = new StringBuffer();
		for  (int i = 0; i <categories.size(); i++){
				buffer.append(categories.get(i).categoryName);
				buffer.append(" ,");
		}
		String result = buffer.toString();
		if (result.length() > 1){
			buffer.delete(buffer.length()-1,buffer.length());
		}
		return buffer.toString();
	}
	
	private String getParamTypeCategories(ArrayList<Category> categories){
		StringBuffer buffer = new StringBuffer();
		Log.v("선택한 카테고리 수 " , categories.size() + " 개");
		for  (int i = 0; i <categories.size(); i++){
				buffer.append(CommonUtils.int2string(categories.get(i).categoryId));
		}
		return buffer.toString();
	}

	private void initListener() {
		findViewById(R.id.activity_profile_image_get_btn).setOnClickListener(this);
		findViewById(R.id.activity_profile_nickname_btn).setOnClickListener(this);
		findViewById(R.id.activity_profile_email_btn).setOnClickListener(this);
		findViewById(R.id.activity_profile_target_btn).setOnClickListener(this);
		findViewById(R.id.activity_profile_mycategory_btn).setOnClickListener(this);
		((Button)findViewById(R.id.activity_profile_submit_btn)).setOnClickListener(this);
	}
	

	
	/** 임시 저장 파일의 경로를 반환 */
	private Uri getTempUri() {
        return Uri.fromFile(getTempFile());
	}
	
	/** 외장메모리에 임시 이미지 파일을 생성하여 그 파일의 경로를 반환  */
	private File getTempFile() {
        if (isSDCARDMounted()) {
            File f = new File(mTmpFile);
            try {
                f.createNewFile();      
            } catch (IOException e) { }
            return f;
        } else
            return null;
    }
	
	/** SD카드가 마운트 되어 있는지 확인 */
    private boolean isSDCARDMounted() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED))
            return true;
 
        return false;
    }
    
    
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.activity_profile_image_get_btn:
			showProfileDialog(R.string.selectimage, android.R.drawable.ic_input_add, DIALOG_PROFILE_IMAGE);
			break;
		case R.id.activity_profile_nickname_btn:
			AndroUtils.showToastMessage(this, R.string.banned_modify_nickname, Toast.LENGTH_SHORT);
			break;
		case R.id.activity_profile_email_btn:
			AndroUtils.showToastMessage(this, R.string.banned_modify_email, Toast.LENGTH_SHORT);
			break;
		case R.id.activity_profile_target_btn:
			showProfileDialog(R.string.activity_profile_input_purpose_text, android.R.drawable.ic_input_add, DIALOG_PURPOSE);
			break;
		case R.id.activity_profile_mycategory_btn:
			showProfileDialog(R.string.activity_profile_input_category_text, android.R.drawable.ic_input_add, DIALOG_MYCATEGOTY);
			break;
		case R.id.activity_profile_submit_btn:
			modifyProfile();
			break;
		}
	}
	
	private void modifyProfile() {
		if (!waitProgressDialog.isShowing()){
			waitProgressDialog.show();
		}
		
		userService.setOnUserCallback(this);
		userService.modifyProfile(getParamTypeCategories(mCategoryList), mPurpose, mEmail, mName, mProfileImagePath);
	}
	
	public void onUserServiceCallback(BaseResponseObject object) {
		if (waitProgressDialog.isShowing()){
			waitProgressDialog.dismiss();
		}
		if (object.resultCode == Service.RESULT_FAIL) {
			AndroUtils.showToastMessage(this, object.resultCode + " : " + object.resultMsg, Toast.LENGTH_SHORT);
			Log.v("수정 에러 " , object.resultCode + " : " + object.resultMsg);
			return;
		}
		
		switch(object.requestType){
		case Service.REQUEST_TYPE_MODIFY_PROFILE:
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
										modifyProfile();
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
			
			AndroUtils.showToastMessage(ActivityProfile.this, R.string.profile_modify_ok, Toast.LENGTH_SHORT);
			Session.getInstance().user.setMyCategoris(getParamTypeCategories(mCategoryList));
			Session.getInstance().user.purpose = mPurpose;
			Session.getInstance().user.nickName = mName;
			finish();
		break;
		}
		
	}

	private void showProfileDialog(int title, int drawableId, int type) {
		DialogFragment  newFragment = ProfileDialogFragment.newInstance(title, drawableId, type);
		FragmentManager fm = getSupportFragmentManager(); 
		newFragment.show(fm,"dialog");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
		case CROP_FROM_IMAGE:
			/*주석의 코드는 크랍이미지를 파일로 저장하지 않고, bundle 로 bitmap를 가져오는 경우 
			final Bundle extras = data.getExtras();
			Bitmap photo = null;
			if (extras != null) {
				photo = extras.getParcelable("data");
			}*/
			String filePath = getTempUri().getPath();
			Bitmap bitmap = BitmapFactory.decodeFile(filePath);
			Log.v("크랍이미지 정보" ,"width==>"+ bitmap.getWidth() + " , height ===> " + bitmap.getHeight());
			onProfileImageChange(bitmap, filePath);
			
			/*File f = new File(getTempUri().getPath());
			촬영된 원본 이미지를 삭제한다. 만약 이 촬영이미지를 서버에 업로드 하는 경우 삭제하면 안된다.
			혹은 삭제한다면 생성된 촬영 비트맵이나 crop된 이미지를 스트림으로 변환해야 한다.
			if (f.exists())
				f.delete();
			*/
			break;
			
		case PICK_FROM_ALBUM:
			mImageCaptureUri = data.getData();
			mTmpImagePath = AndroUtils.getRealImagePath(this, mImageCaptureUri);
			mTmpFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp"+ "." + CommonUtils.getFileExtension(mTmpImagePath);
			
			Log.v("선택한 파일 이름", "" + mTmpImagePath);
		    Intent i  = new Intent("com.android.camera.action.CROP");
			i.setDataAndType(mImageCaptureUri, "image/*");
			i.putExtra("outputX", 800);
			i.putExtra("outputY", 600);
			i.putExtra("aspectX", 1.3);
			i.putExtra("aspectY", 1);
			i.putExtra("scale", true);
			i.putExtra("output", getTempUri());
			startActivityForResult(i, CROP_FROM_IMAGE);
			break;
	

		case PICK_FROM_CAMERA:
			Intent intent = new Intent("com.android.camera.action.CROP");
			intent.setDataAndType(mImageCaptureUri, "image/*");
			intent.putExtra("outputX", 800);
			intent.putExtra("outputY", 600);
			intent.putExtra("aspectX", 1.3);
			intent.putExtra("aspectY", 1);
			intent.putExtra("scale", true);
			/*이 인텐트를 줄 경우 크롭화면에서 저장을 누를 경우 경우  비트앱을 Bundle를 통해 가져오겠다는 의미*/
			//intent.putExtra("return-data", true);
			//아래의 인텐트는 크롭화면에서 저장을 누를 경우 파일로 저장하겠다는 의미 로 지정한 Uril 크랍된 이미지가 저장된다.
			intent.putExtra("output", getTempUri());
			startActivityForResult(intent, CROP_FROM_IMAGE);
			break;
		}	
	}
	
	private void getImageFromCamera() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		mTmpFile = Environment.getExternalStorageDirectory().getAbsolutePath() +  "/tmp.jpg";
		mImageCaptureUri = Uri.fromFile(new File(mTmpFile));
		intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,mImageCaptureUri);
		startActivityForResult(intent,PICK_FROM_CAMERA);
	}

	private void getImageFromAlbum() {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType("image/*");
		startActivityForResult(i, PICK_FROM_ALBUM);
	}
	
	public void onProfileImageChange(Bitmap bit, String path){
		mProfileImagePath = path;
		mProfileImageView.setImageBitmap(bit);
	}
	
	public void onNicknameChange(String val){
		mName = val.trim();
		mNickNameTextView.setText(val.trim());
		
	}
	
	public void onEmailChange(String val){
		mEmail = val.trim();
		mEmailTextView.setText(val.trim());
	}

	public void onPurposeChange(String val){
		mPurpose = val.trim();
		mPurposeTextView .setText(val.trim());
	}
	
	public void onMyCategoryChange(boolean[] selectedCategories){
		mCategoryList.clear();
		for (int i = 0; i <selectedCategories.length; i++){
			if (selectedCategories[i]){
				Category category = new Category(appCategories.get(i).categoryId,appCategories.get(i).categoryName,"카테고리 설명" );
				mCategoryList.add(category);
			}
		}
		String resultStr = getCategorieStr(mCategoryList);
		mMyCategoryTextView.setText(resultStr);
	}
	
	
	
	public static class ProfileDialogFragment extends DialogFragment {
        public static ProfileDialogFragment newInstance(int title, int drawableId, int type) {
            ProfileDialogFragment frag = new ProfileDialogFragment();
            Bundle args = new Bundle();
            args.putInt("title", title);
            args.putInt("icon",drawableId);
            args.putInt("type", type);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
             setStyle(DialogFragment.STYLE_NO_FRAME, 0);
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	final LayoutInflater inflator  = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	final LinearLayout common1 = (LinearLayout)inflator.inflate(R.layout.common_modify, null);
        	
        	final int title = getArguments().getInt("title");
            final int icon = getArguments().getInt("icon");
            final int type = getArguments().getInt("type");
            
            Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title);
            builder.setIcon(icon);
            
            switch(type){
            case DIALOG_PROFILE_IMAGE:
            	builder.setItems(R.array.profile_imget_get_mthod,new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						if (which == PICK_FROM_ALBUM) {
							((ActivityProfile)getActivity()).getImageFromAlbum();
						} else if (which == PICK_FROM_CAMERA) {
							((ActivityProfile)getActivity()).getImageFromCamera();
						}
					}
				});
            	break;
            case DIALOG_NICNAME:
            	((EditText)common1.findViewById(R.id.common_edittext)).setText(((ActivityProfile)getActivity()).mName);
            	builder.setView(common1);
            	break;
            case DIALOG_EMAIL:
            	((EditText)common1.findViewById(R.id.common_edittext)).setText(((ActivityProfile)getActivity()).mEmail);
            	((EditText)common1.findViewById(R.id.common_edittext)).setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            	builder.setView(common1);
            	break;
            case DIALOG_PURPOSE:
            	((EditText)common1.findViewById(R.id.common_edittext)).setText(((ActivityProfile)getActivity()).mPurpose);
            	builder.setView(common1);
            	break;
            case DIALOG_MYCATEGOTY:
            	final boolean[] flags = new boolean[((ActivityProfile)getActivity()).appCategories.size()];
            	final ArrayList<Category> myCategories = ((ActivityProfile)getActivity()).mCategoryList;
            	final ArrayList<Category> defaultCategories = ((ActivityProfile)getActivity()).appCategories;
            	
            	for (Category cat : myCategories){
            		flags[cat.categoryId-1] = true;
            	}
            	final String[] displayCategoryList =  new String[defaultCategories.size()];
            	
            	for (int i = 0; i <defaultCategories.size(); i++){
            		displayCategoryList[i] = defaultCategories.get(i).categoryName;
            	}
            	builder.setMultiChoiceItems(displayCategoryList, flags, new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						flags[which] = isChecked;
						
					}
				})
					.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						((ActivityProfile)getActivity()).onMyCategoryChange(flags);
					}
				})
				.setNegativeButton(R.string.cancel, null);
            	break;
            }
            
            if (type == DIALOG_NICNAME || type == DIALOG_PURPOSE ||  type == DIALOG_EMAIL ){
     	       builder.setPositiveButton(R.string.confirm,
     	                    new DialogInterface.OnClickListener() {
     	                         public void onClick(DialogInterface dialog, int whichButton) {
     	                        	    String val =  ((EditText)common1.findViewById(R.id.common_edittext)).getText().toString();                       
     	                        	    switch(type){
     	                        	 	case DIALOG_NICNAME:
     	                        	 		((ActivityProfile)getActivity()).onNicknameChange(val);
     	                        	 		break;
     	                        	 	case DIALOG_PURPOSE:
     	                        	 		((ActivityProfile)getActivity()).onPurposeChange(val);
     	                        	 		break;
     	                        	 	case DIALOG_EMAIL:
     	                        	 		((ActivityProfile)getActivity()).onEmailChange(val);
     	                        	 		break;
     	                        	 	}                  
     	                         }
     	                      }
     	                    )
     	                    .setNegativeButton(R.string.cancel,null); 
                }

           return builder.create();
        }

    }
	
}
