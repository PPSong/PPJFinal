package com.penn.ppj;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by penn on 29/05/2017.
 */

public class PPApplication extends Application {
    //preference keys
    public static String AUTH_BODY = "AUTH_BODY";
    public static String CUR_USER_ID = "CUR_USER_ID";
    public static String BAIDU_API_URL = "BAIDU_API_URL";
    public static String BAIDU_AK_BROWSER = "BAIDU_AK_BROWSER";
    public static String SOCKET_URL = "SOCKET_URL";

    public static final int REQUEST_VERIFY_CODE_INTERVAL = 5;

    private static String APP_NAME = "PPJ";
    private static final String LATEST_GEO = "LATEST_GEO";
    private static Context appContext;
    private static final String qiniuBase = "http://7xu8w0.com1.z0.glb.clouddn.com/";
    //pptodo 修改默认地理位置
    private static final String DEFAULT_GEO = "121.0,31.0";
    private static ProgressDialog progressDialog;

    private static Socket socket;

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

    @BindingAdapter({"bind:image40"})
    public static void image40(final ImageView imageView, String pic) {
        setImageViewResource(imageView, pic, 40);
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
        return !TextUtils.isEmpty(getPrefStringValue(AUTH_BODY, ""));
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

    //删除pref值
    public static void removePrefItem(String key) {
        getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().remove(key).apply();
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
        removePrefItem(AUTH_BODY);
        removePrefItem(CUR_USER_ID);
        removePrefItem(BAIDU_API_URL);
        removePrefItem(BAIDU_AK_BROWSER);
        removePrefItem(SOCKET_URL);
        stopSocket();

        //发出已logout广播
        EventBus.getDefault().post(new UserLogoutEvent());
    }

    //login
    public static Observable<String> login(final String phone, final String pwd) {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", phone)
                .put("pwd", pwd);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("user.login", jBody.getJSONObject());

        return apiResult
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        String userId = ppFromString(s, "data.userid").getAsString();

                        //存储token相关信息到preference
                        String authBody = new JSONObject()
                                .put("userid", userId)
                                .put("token", ppFromString(s, "data.token").getAsString())
                                .put("tokentimestamp", ppFromString(s, "data.tokentimestamp").getAsLong())
                                .toString();
                        setPrefStringValue(AUTH_BODY, authBody);
                        setPrefStringValue(CUR_USER_ID, userId);

                        //创建登录用户自己的realm数据库
                        initRealm(userId);

                        try (Realm realm = Realm.getDefaultInstance()) {

                            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();

                            realm.beginTransaction();

                            try {
                                if (currentUser == null) {
                                    currentUser = new CurrentUser();
                                    currentUser.setUserId(userId);
                                }

                                realm.insertOrUpdate(currentUser);

                                realm.commitTransaction();
                            } catch (Exception e) {
                                error(e.toString());
                                realm.cancelTransaction();
                            }
                        }

                        return PPRetrofit.getInstance().api("user.startup", null);
                    }
                })
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        //设置登录用户相关preference
                        setPrefStringValue(BAIDU_AK_BROWSER, ppFromString(s, "data.settings.geo.ak_browser").getAsString());
                        setPrefStringValue(BAIDU_API_URL, ppFromString(s, "data.settings.geo.api").getAsString());
                        setPrefStringValue(SOCKET_URL, ppFromString(s, "data.settings.socket.host").getAsString() + ":" + ppFromString(s, "data.settings.socket.port").getAsInt());

                        //设置CurrentUser
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.beginTransaction();

                            try {
                                CurrentUser currentUser = realm.where(CurrentUser.class)
                                        .findFirst();

                                currentUser.setNickname(ppFromString(s, "data.userInfo.nickname").getAsString());
                                currentUser.setSex(ppFromString(s, "data.userInfo.gender").getAsInt());
                                currentUser.setBirthday(ppFromString(s, "data.userInfo.birthday").getAsLong());
                                currentUser.setAvatar(ppFromString(s, "data.userInfo.head", PPValueType.STRING).getAsString());

                                realm.commitTransaction();
                            } catch (Exception e) {
                                error(e.toString());
                                realm.cancelTransaction();
                            }
                        }

                        return refreshRelatedUsers();
                    }
                });
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

    //显示modal提示框
    public static void showProgressDialog(Context context, String body, String title) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
        progressDialog = ProgressDialog.show(context, title, body);
    }

    //取消modal提示框
    public static void hideProgressDialog() {
        progressDialog.cancel();
    }

    //网络重新获取连接后需要刷新的动作
    public static void networkConnectNeedToRefresh() {
    }

    //得到socket push后需要做的动作
    public static void getSocketPush() {
    }

    //刷新CurrentUser
    public static void refreshCurrentUser() {
        PPRetrofit.getInstance().api("user.startup", null)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();
                                    realm.beginTransaction();

                                    try {
                                        currentUser.setNickname(ppFromString(s, "data.userInfo.nickname").getAsString());
                                        currentUser.setSex(ppFromString(s, "data.userInfo.gender").getAsInt());
                                        currentUser.setBirthday(ppFromString(s, "data.userInfo.birthday").getAsLong());
                                        currentUser.setAvatar(ppFromString(s, "data.userInfo.head", PPValueType.STRING).getAsString());

                                        realm.commitTransaction();
                                    } catch (Exception e) {
                                        error(e.toString());
                                        realm.cancelTransaction();
                                    }
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                            }
                        }
                );
    }

    //校验password
    public static String isPasswordValid(String password) {
        String error = "";
        if (TextUtils.isEmpty(password)) {
            error = getContext().getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\w{6,12}", password.toString())) {
            error = getContext().getString(R.string.error_invalid_password);
        }

        return error;
    }

    //校验nickname
    public static String isNicknameValid(String nickname) {
        String error = "";
        if (TextUtils.isEmpty(nickname)) {
            error = getContext().getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\w{3,12}", nickname.toString())) {
            error = getContext().getString(R.string.error_invalid_nickname);
        }

        return error;
    }

    //校验username
    public static String isUsernameValid(String phone) {
        String error = "";
        if (TextUtils.isEmpty(phone)) {
            error = getContext().getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\d{11}", phone)) {
            error = getContext().getString(R.string.error_invalid_phone);
        }

        return error;
    }

    //校验验证码
    public static String isVerifyCodeValid(String verifyCode) {
        String error = "";
        if (TextUtils.isEmpty(verifyCode)) {
            error = getContext().getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\d{6}", verifyCode)) {
            error = getContext().getString(R.string.error_invalid_verify_code);
        }

        return error;
    }

    //刷新我的fans
    public static Observable<String> refreshFans() {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("before", "0");

        return PPRetrofit.getInstance()
                .api("friend.myFans", jBody.getJSONObject());
    }

    //刷新我的follows
    public static Observable<String> refreshFollows() {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("before", "0");

        return PPRetrofit.getInstance()
                .api("friend.myFollows", jBody.getJSONObject());
    }

    //刷新我的friends
    public static Observable<String> refreshFriends() {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("needInfo", "1");

        return PPRetrofit.getInstance()
                .api("friend.mine", jBody.getJSONObject());
    }

    //解析保存fans, follows, friends
    public static void parseAndSaveToLocalDB(String s, RelatedUserType relatedUserType) throws Exception {
        PPWarn ppWarn = ppWarning(s);
        if (ppWarn != null) {
            throw new Exception(ppWarn.msg);
        }

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            long now = System.currentTimeMillis();

            try {
                if (relatedUserType == RelatedUserType.FRIEND) {
                    //friends
                    JsonArray ja = ppFromString(s, "data.list").getAsJsonArray();

                    for (int i = 0; i < ja.size(); i++) {
                        RelatedUser relatedUser = new RelatedUser();
                        relatedUser.setType(RelatedUserType.FRIEND);
                        relatedUser.setUserId(ppFromString(s, "data.list." + i + ".id").getAsString());
                        relatedUser.setKey();
                        relatedUser.setNickname(ppFromString(s, "data.list." + i + ".nickname").getAsString());
                        relatedUser.setAvatar(ppFromString(s, "data.list." + i + ".head").getAsString());
                        relatedUser.setCreateTime(ppFromString(s, "data.list." + i + ".time").getAsLong());
                        relatedUser.setLastVisitTime(now);

                        realm.insertOrUpdate(relatedUser);
                    }
                } else {
                    //fans, follows
                    JsonArray ja = ppFromString(s, "data").getAsJsonArray();

                    for (int i = 0; i < ja.size(); i++) {
                        RelatedUser relatedUser = new RelatedUser();
                        relatedUser.setType(relatedUserType);
                        relatedUser.setUserId(ppFromString(s, "data." + i + ".id").getAsString());
                        relatedUser.setKey();
                        relatedUser.setNickname(ppFromString(s, "data." + i + ".nickname").getAsString());
                        relatedUser.setAvatar(ppFromString(s, "data." + i + ".head").getAsString());
                        relatedUser.setCreateTime(ppFromString(s, "data." + i + ".time").getAsLong());
                        relatedUser.setLastVisitTime(now);

                        realm.insertOrUpdate(relatedUser);
                    }
                }

                //把服务器上已删除的user从本地删掉
                realm.where(RelatedUser.class).equalTo("type", relatedUserType.toString()).notEqualTo("lastVisitTime", now).findAll().deleteAllFromRealm();

                realm.commitTransaction();

            } catch (Exception e) {
                realm.cancelTransaction();
                throw new Exception(e.toString());
            }
        }
    }

    //同时刷新follows,fans,friends
    public static Observable<String> refreshRelatedUsers() {
        return Observable
                .zip(
                        refreshFans(),
                        refreshFollows(),
                        refreshFriends(),
                        new Function3<String, String, String, String>() {

                            @Override
                            public String apply(@NonNull String s, @NonNull String s2, @NonNull String s3) throws Exception {
                                parseAndSaveToLocalDB(s, RelatedUserType.FAN);
                                parseAndSaveToLocalDB(s2, RelatedUserType.FOLLOW);
                                parseAndSaveToLocalDB(s3, RelatedUserType.FRIEND);

                                return "OK";
                            }
                        }
                );
    }

    //------------------------------private------------------------------
    //设置DefaultConfiguration for 登录用户
    public static void initRealm(String userId) {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name(userId + ".realm")
                .build();
        //清除当前用户的数据文件, 测试用
        boolean clearData = false;
        if (clearData) {
            Realm.deleteRealm(config);
        }

        Realm.setDefaultConfiguration(config);
    }

    public static void startSocket() throws Exception {
        //如果socket已正常连接则返回
        if (socket != null && socket.connected()) {
            return;
        }

        socket = IO.socket(getPrefStringValue(SOCKET_URL, ""));
        socket
                .on(
                        Socket.EVENT_CONNECT,
                        new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                Log.v("ppLog1000", "Socket.EVENT_CONNECT");
                                try {
                                    JSONObject tmpBody = new JSONObject();

                                    tmpBody.put("_sess", new JSONObject(getPrefStringValue(AUTH_BODY, "")));

                                    socket.emit("$init", tmpBody);

                                } catch (JSONException e) {
                                    Log.v("pplog", "startSocket error:" + e);
                                }
                            }

                        })
                .on(
                        "$init",
                        new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                String msg = args[0].toString();
                                Log.v("ppLog1000", "$init from server:" + msg);
                            }
                        })
                .on(
                        "$kick",
                        new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                String msg = args[0].toString();
                                Log.v("ppLog1000", "$kick from server:" + msg);
                            }
                        })
                .on(
                        "sync",
                        new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                getSocketPush();
                            }
                        })
                .on(
                        Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                Log.v("ppLog1000", "Socket.EVENT_DISCONNECT");
                            }

                        });

        socket.connect();
    }

    private static void stopSocket() {
        if (socket != null && socket.connected()) {
            socket.close();
        }

        socket = null;
    }
}
