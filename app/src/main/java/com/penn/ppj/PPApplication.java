package com.penn.ppj;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.util.PPWarn;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;
import io.realm.Realm;

/**
 * Created by penn on 29/05/2017.
 */

public class PPApplication extends Application {
    public static String AUTH_BODY = "AUTH_BODY";

    private static String APP_NAME = "PPJ";
    private static final String LATEST_GEO = "LATEST_GEO";
    private static Context appContext;
    private static final String qiniuBase = "http://7xu8w0.com1.z0.glb.clouddn.com/";
    //pptodo 修改默认地理位置
    private static final String DEFAULT_GEO = "121.0,31.0";

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        Realm.init(this);
    }

    //-----helper-----
    public static Context getContext() {
        return appContext;
    }

    @BindingAdapter({"bind:image180"})
    public static void image180(final ImageView imageView, String pic) {
        setImageViewResource(imageView, pic, 180);
    }

    public static void setImageViewResource(final ImageView imageView, String pic, int size) {
        Picasso.with(getContext())
                .load(getImageUrl(pic, size))
                .into(imageView);
    }

    private static String getImageUrl(String imageName, int size) {
        //如果为空则用默认图片
        if (TextUtils.isEmpty(imageName)) {
            imageName = "default";
        }

        if (imageName.startsWith("http")) {
            //如果直接是网络地址则直接用
            return imageName;
        } else {
            String result = qiniuBase + imageName + "?imageView2/1/w/" + size + "/h/" + size + "/interlace/1/";
            return result;
        }
    }

    //根据位置计算图片背景色
    public static int getMomentOverviewBackgroundColor(int position) {
        TypedArray ta = getContext().getResources().obtainTypedArray(R.array.loading_placeholders_dark);
        int[] colors = new int[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            colors[i] = ta.getColor(i, 0);
        }
        return colors[position % ta.length()];
    }

    //判断是否为登录状态
    public static boolean isLogin() {
        return false;
    }

    //去登录或注册
    public static void goLogin(Activity activity) {

    }

    //以每图180dp算,计算屏幕能容纳的列数
    public static int calculateNoOfColumns() {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 180);
        return noOfColumns;
    }

    //取得最新保留的地理位置
    public static String getLatestGeoString() {
        return getPrefStringValue(LATEST_GEO, DEFAULT_GEO);
    }

    //设置pref值
    public static void setPrefStringValue(String key, String value) {
        getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    //取pref值
    public static String getPrefStringValue(String key, String defaultValue) {
        return getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    //解析json字符串(带空的默认值)
    public static JsonElement ppFromString(String json, String path, PPValueType type) {
        JsonElement jsonElement = ppFromString(json, path);
        if (jsonElement == null) {
            switch (type) {
                case ARRAY:
                    return new JsonArray();
                case INT:
                    return new JsonPrimitive(0);
                case STRING:
                    return new JsonPrimitive("");
                default:
                    return null;
            }
        }

        return jsonElement;
    }

    //解析json字符串(不带空的默认值,如空会报错)
    public static JsonElement ppFromString(String json, String path) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement item = parser.parse(json);
            if (path == null || path.length() == 0 || Pattern.matches("\\.+", path)) {
                //Log.v("ppLog", "解析整个json String");
                return item;
            }
            String[] seg = path.split("\\.");
            for (int i = 0; i < seg.length; i++) {
                if (i > 0) {
                    //Log.v("ppLog", "解析完毕:" + seg[i - 1]);
                    //Log.v("ppLog", "-------");
                }
                //Log.v("ppLog", "准备解析:" + seg[i]);
                if (seg[i].length() == 0) {
                    //""情况
                    //Log.v("ppLog", "解析空字符串的path片段, 停止继续解析");
                    return null;
                }
                if (item != null) {
                    //当前path片段item不为null
                    //Log.v("ppLog", "当前path片段item不为null");
                    if (item.isJsonArray()) {
                        //当前path片段item为数组
                        //Log.v("ppLog", "当前path片段item为数组");
                        String regex = "\\d+";
                        if (Pattern.matches("\\d+", seg[i])) {
                            //当前path片段描述为数组格式
                            //Log.v("ppLog", "当前path片段描述为数组格式");
                            item = item.getAsJsonArray().get(Integer.parseInt(seg[i]));
                        } else {
                            //当前path片段描述不为数组格式
                            //Log.v("ppLog", "当前path片段描述不为数组格式");
                            //Log.v("ppLog", "path中间片段描述错误:" + seg[i] + ", 停止继续解析");
                            return null;
                        }
                    } else if (item.isJsonObject()) {
                        //当前path片段item为JsonObject
                        //Log.v("ppLog", "当前path片段item为JsonObject");
                        item = item.getAsJsonObject().get(seg[i]);
                    } else {
                        //当前path片段item为JsonPrimitive
                        //Log.v("ppLog", "当前path片段item为JsonPrimitive");
                        //Log.v("ppLog", "path中间片段取值为JsonPrimitive, 停止继续解析");
                        return null;
                    }
                } else {
                    //当前path片段item为null
                    //Log.v("ppLog", "当前path片段item为null");
                    Log.v("ppLog", path + ":path中间片段取值为null, 停止继续解析");
                    return null;
                }
            }
            return item;
        } catch (Exception e) {
            Log.v("ppLog", "Json解析错误" + e);
            return null;
        }
    }

    //判断jb服务器返回数据中是否包含异常信息
    public static PPWarn ppWarning(String jServerResponse) {
        int code = ppFromString(jServerResponse, "code", PPValueType.INT).getAsInt();
        if (code != 1) {
            return new PPWarn(jServerResponse);
        } else {
            return null;
        }
    }

    //error提示
    public static void error(final String error) {
        Log.v("pplog", error);
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(getContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (error.equalsIgnoreCase("java.lang.Exception: NO_PERMISSION")) {
                    Toasty.error(getContext(), getContext().getString(R.string.login_please), Toast.LENGTH_LONG, true).show();
                    //如果当前为登录状态,说明被人踢下,需要logout后重新登录
                    if (isLogin()) {
                        logout();
                        noticeLogout();
                    }
                } else {
                    Toasty.error(getContext(), error, Toast.LENGTH_LONG, true).show();
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    //logout
    public static void logout() {
    }

    //发出已logout广播
    public static void noticeLogout() {
        EventBus.getDefault().post(new UserLogoutEvent());
    }

    //取得状态栏和toolbar的总高度
    public static int getStatusBarAddActionBarHeight() {
        int statusBarHeight = getStatusBarHeight();
        int toolBarHeight = 0;

        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            toolBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getContext().getResources().getDisplayMetrics());
        }
        return statusBarHeight + toolBarHeight;
    }

    //取得状态栏高度
    public static int getStatusBarHeight() {
        int result = 0;
        int resourceId = getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getContext().getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }
}
