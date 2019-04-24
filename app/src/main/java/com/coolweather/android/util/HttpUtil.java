package com.coolweather.android.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    //发起网络请求仅需要调用此方法
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();//实例化
        Request request = new Request.Builder().url(address).build();//创建请求对象
        client.newCall(request).enqueue(callback);//发送请求
    }
}
