package com.penn.ppj.util;

import android.util.Log;

import com.penn.ppj.PPApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Created by penn on 07/04/2017.
 */

public class PPRetrofit

{
    private static final String BASE_URL = "http://jbapp.magicfish.cn/";
    private static PPRetrofit ppRetrofit = new PPRetrofit();
    private PPJBService ppJBService;

    public PPRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(PPMsgPackConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(getRequestHeader())
                .build();

        ppJBService = retrofit.create(PPJBService.class);
    }

    public static PPRetrofit getInstance() {
        return ppRetrofit;
    }

    public Observable<String> api(String apiName, JSONObject jBody) {
        String request = "";
        try {
            request = new JSONObject()
                    .put("method", apiName)
                    .put("data", jBody == null ? "" : jBody)
                    .put("auth", PPApplication.getPrefStringValue(PPApplication.AUTH_BODY, ""))
                    .toString();
        } catch (JSONException e) {
            Log.v("ppLog", "api data error:" + e);
        }
        return ppJBService.api(request);
    }

    private OkHttpClient getRequestHeader() {
        //pptodo 以下timeout好像有时不灵
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();

        return httpClient;
    }
}
