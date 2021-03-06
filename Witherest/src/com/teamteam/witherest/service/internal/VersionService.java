package com.teamteam.witherest.service.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import android.os.Message;
import android.util.Log;

import com.teamteam.witherest.model.Session;
import com.teamteam.witherest.service.callback.UserServiceCallback;
import com.teamteam.witherest.service.callback.VersionServiceCallback;
import com.teamteam.witherest.service.callback.object.BaseResponseObject;

public class VersionService extends Service{
	public static final String tag = "version_check_reponse_text";
	private VersionServiceCallback callback;
	
	public VersionService(HttpClient httpClient, ServiceHandler  handler) {
		super(httpClient, handler);
	}

	public void setOnVersionCallback(VersionServiceCallback callback){
		this.callback = callback;
		handler.setVersionServiceCallback(callback);
	}
	
	public void checkVersion(){
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put(REQUEST_TYPE_STRING,String.valueOf(REQUEST_TYPE_CHECK_VERSION));
		sendGet(VERSION_CHECK_URL, paramMap);
	}
}
