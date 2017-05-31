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

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.penn.ppj.messageEvent.MomentPublishEvent;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Message;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.CommentStatus;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.R.attr.id;
import static com.penn.ppj.R.string.moment;

/**
 * Created by penn on 29/05/2017.
 */

public class PPApplication extends Application {
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();

    //preference keys
    public static String AUTH_BODY = "AUTH_BODY";
    public static String CUR_USER_ID = "CUR_USER_ID";
    public static String BAIDU_API_URL = "BAIDU_API_URL";
    public static String BAIDU_AK_BROWSER = "BAIDU_AK_BROWSER";
    public static String SOCKET_URL = "SOCKET_URL";

    public static final int DASHBOARD_MOMENT_PAGE_SIZE = 20;
    public static final int MESSAGE_PAGE_SIZE = 20;

    public static final int REQUEST_VERIFY_CODE_INTERVAL = 5;

    private static String APP_NAME = "PPJ";
    private static final String LATEST_GEO = "LATEST_GEO";
    private static final String LATEST_ADDRESS = "LATEST_ADDRESS";

    private static Context appContext;
    private static final String qiniuBase = "http://7xu8w0.com1.z0.glb.clouddn.com/";
    //pptodo 修改默认地理位置
    private static final String DEFAULT_GEO = "121.0,31.0";
    private static ProgressDialog progressDialog;

    private static Socket socket;

    private static Configuration config = new Configuration.Builder().build();
    private static UploadManager uploadManager = new UploadManager(config);

    class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            //获取定位结果
            StringBuffer sb = new StringBuffer(256);

            sb.append("time : ");
            sb.append(location.getTime());    //获取定位时间

            sb.append("\nerror code : ");
            sb.append(location.getLocType());    //获取类型类型

            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());    //获取纬度信息

            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());    //获取经度信息

            if (location.getLocType() != BDLocation.TypeOffLineLocation) {
                setPrefStringValue(LATEST_GEO, String.format("%.9f", location.getLongitude()) + "," + String.format("%.9f", location.getLatitude()));
                setPrefStringValue(LATEST_ADDRESS, location.getAddrStr());
            }

            sb.append("\nradius : ");
            sb.append(location.getRadius());    //获取定位精准度

            if (location.getLocType() == BDLocation.TypeGpsLocation) {

                // GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());    // 单位：公里每小时

                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());    //获取卫星数

                sb.append("\nheight : ");
                sb.append(location.getAltitude());    //获取海拔高度信息，单位米

                sb.append("\ndirection : ");
                sb.append(location.getDirection());    //获取方向信息，单位度

                sb.append("\naddr : ");
                sb.append(location.getAddrStr());    //获取地址信息

                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {

                // 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());    //获取地址信息

                sb.append("\noperationers : ");
                sb.append(location.getOperators());    //获取运营商信息

                sb.append("\ndescribe : ");
                sb.append("网络定位成功");

            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {

                // 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");

            } else if (location.getLocType() == BDLocation.TypeServerError) {

                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");

            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {

                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");

            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {

                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");

            }

            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());    //位置语义化信息

            List<Poi> list = location.getPoiList();    // POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }

//            Log.i("BaiduLocationApiDem", sb.toString());
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        Realm.init(this);

        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);
        //注册监听函数

        initLocation();

        mLocationClient.start();
    }

    //-----helper-----
    public static Context getContext() {
        return appContext;
    }

    @BindingAdapter({"bind:ppReferenceTime"})
    public static void setTimeAgo(final RelativeTimeTextView relativeTimeTextView, long time) {
        if (time > 0) {
            relativeTimeTextView.setReferenceTime(time);
        }
    }

    @BindingAdapter({"bind:imageData"})
    public static void setImageViewDataResource(final ImageView imageView, byte[] pic) {
        if (pic == null) {
            imageView.setImageResource(android.R.color.transparent);
            return;
        }
        Bitmap bmp = BitmapFactory.decodeByteArray(pic, 0, pic.length);
        imageView.setImageBitmap(bmp);
    }

    @BindingAdapter({"bind:avatarImageUrl"})
    public static void setAvatarImageViewResource(final ImageView imageView, String pic) {
        setImageViewResource(imageView, pic, 80);
    }

    @BindingAdapter({"bind:imagePic180"})
    public static void imagePic180(final ImageView imageView, Pic pic) {
        if (pic.getStatus() == PicStatus.NET) {
            setImageViewResource(imageView, pic.getKey(), 180);
        } else {
            byte[] data = pic.getThumbLocalData();
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            imageView.setImageBitmap(bmp);
        }
    }

    @BindingAdapter({"bind:image800"})
    public static void image800(final ImageView imageView, String pic) {
        setImageViewResource(imageView, pic, 800*2);
    }

    @BindingAdapter({"bind:image180"})
    public static void image180(final ImageView imageView, String pic) {
        setImageViewResource(imageView, pic, 180*4);
    }

    @BindingAdapter({"bind:image40"})
    public static void image40(final ImageView imageView, String pic) {
        setImageViewResource(imageView, pic, 40*4);
    }

    public static String get800ImageUrl(String imageName) {
        return getImageUrl(imageName, 800 * 2);
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

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系

        int span = 10000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要

        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps

        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果

        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”

        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到

        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死

        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集

        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要

        mLocationClient.setLocOption(option);
    }

    public static String getCurrentUserId() {
        return getPrefStringValue(CUR_USER_ID, "");
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
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivity(intent);

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

    public static String getLatestAddress() {
        return getPrefStringValue(LATEST_ADDRESS, PPApplication.getContext().getString(R.string.earth));
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

    public static int calculateHeadMinHeight() {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        int height_16_9 = (int) dpWidth * 9 / 16;

        return height_16_9;
    }

    public static int calculateHeadHeight() {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        int height_4_3 = (int) dpWidth * 3 / 4;

        return height_4_3;
    }

    public static int calculateMomentOverHeight() {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        int result = (int) ((dpWidth / calculateNoOfColumns()) * 9 / 16);

        return result;
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
        //更新fans, follows, friends
        doRefreshRelatedUsers();

        //获取最新通知
        getLatestMessages();
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

    public static void doRefreshRelatedUsers() {
        refreshRelatedUsers()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {

                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPApplication.error(throwable.toString());
                            }
                        }
                );
    }

    public static void getLatestMessages() {
        long mostNewMessageCreateTime = 0;
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Message> messages = realm.where(Message.class).findAllSorted("createTime", Sort.DESCENDING);
            if (messages.size() > 0) {
                mostNewMessageCreateTime = messages.get(0).getCreateTime();
            }
        }

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("after", mostNewMessageCreateTime)
                .put("sort", "1");

        final Observable<String> apiResult = PPRetrofit.getInstance().api("message.list", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                int dataSize = 0;
                                //parse and save messages
                                try (Realm realm = Realm.getDefaultInstance()) {

                                    CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();

                                    realm.beginTransaction();

                                    try {
                                        JsonArray ja = ppFromString(s, "data.list").getAsJsonArray();

                                        dataSize = ja.size();

                                        for (int i = 0; i < dataSize; i++) {
                                            Message message = parseMessage(ppFromString(s, "data.list." + i).getAsJsonObject().toString(), currentUser.getNickname(), currentUser.getAvatar());

                                            realm.insertOrUpdate(message);
                                        }

                                        realm.commitTransaction();
                                    } catch (Exception e) {
                                        realm.cancelTransaction();
                                        throw new Exception(e.toString());
                                    }

                                    if (dataSize < MESSAGE_PAGE_SIZE) {
                                        //do nothing
                                    } else {
                                        getLatestMessages();
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

    private static Message parseMessage(String s, String currentUserNickname, String currentUserAvatar) {
        Message message = new Message();
        message.setId(ppFromString(s, "id").getAsString());
        int type = ppFromString(s, "type").getAsInt();
        message.setType(type);
        message.setRead(ppFromString(s, "read").getAsInt() == 1 ? true : false);
        message.setCreateTime(ppFromString(s, "createTime").getAsLong());
        String content = "";

        if (type == 1 || type == 6) {
            message.setMomentId(ppFromString(s, "params.mid").getAsString());
        }

        //nickname, avatar, content
        if (type == 1 || type == 6 || type == 8 || type == 9 || type == 10 || type == 11 || type == 15 || type == 16) {
            message.setId(ppFromString(s, "id").getAsString());
            message.setUserId(ppFromString(s, "params.targetUser.id").getAsString());
            message.setNickname(ppFromString(s, "params.targetUser.nickname").getAsString());
            message.setAvatar(ppFromString(s, "params.targetUser.head").getAsString());
            message.setContent(TextUtils.isEmpty(content) ? PPApplication.getContext().getResources().getString(R.string.empty) : content);

        } else {
            Log.v("pplog", "未处理:" + type);
            message.setNickname(currentUserNickname);
            message.setAvatar(currentUserAvatar);
            content = "未处理:" + s;
            message.setContent(TextUtils.isEmpty(content) ? PPApplication.getContext().getResources().getString(R.string.empty) : content);
            return message;
        }

        if (type == 6) {
            content = message.getNickname() + PPApplication.getContext().getString(R.string.like_your_moment);
        } else if (type == 1) {
            content = message.getNickname() + PPApplication.getContext().getString(R.string.reply_your_moment);
        } else if (type == 8) {
            content = message.getNickname() + PPApplication.getContext().getString(R.string.follow_you_footprint);
        } else if (type == 16) {
            content = String.format(PPApplication.getContext().getString(R.string.you_sb_shoulder_meet), message.getNickname());
        } else if (type == 10) {
            content = String.format(PPApplication.getContext().getString(R.string.you_get_sb_mail), message.getNickname());
        } else if (type == 15) {
            content = String.format(PPApplication.getContext().getString(R.string.you_follow_sb_moment), message.getNickname());
        } else if (type == 9) {
            content = String.format(PPApplication.getContext().getString(R.string.you_and_sb_be_friend), message.getNickname());
        } else if (type == 11) {
            content = String.format(PPApplication.getContext().getString(R.string.get_sb_reply_mail), message.getNickname());
        }

        message.setContent(content);
        return message;
    }

    public static void getLatestMoments() {
        long mostNewMomentCreateTime = 0;
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Moment> moments = realm.where(Moment.class).findAllSorted("createTime", Sort.DESCENDING);
            if (moments.size() > 0) {
                mostNewMomentCreateTime = moments.get(0).getCreateTime();
            }
        }

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("after", mostNewMomentCreateTime)
                .put("sort", "1");

        final Observable<String> apiResult = PPRetrofit.getInstance().api("timeline.mine", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                int dataSize = 0;
                                //parse and save moments
                                try (Realm realm = Realm.getDefaultInstance()) {

                                    realm.beginTransaction();

                                    try {
                                        JsonArray ja = ppFromString(s, "data.timeline").getAsJsonArray();

                                        dataSize = ja.size();

                                        for (int i = 0; i < dataSize; i++) {
                                            long createTime = ppFromString(s, "data.timeline." + i + "._info.createTime").getAsLong();

                                            Moment moment = new Moment();
                                            moment.setKey(createTime + "_" + ppFromString(s, "data.timeline." + i + "._info._creator.id").getAsString());
                                            moment.setId(ppFromString(s, "data.timeline." + i + ".id").getAsString());
                                            moment.setUserId(ppFromString(s, "data.timeline." + i + "._info._creator.id").getAsString());
                                            moment.setCreateTime(createTime);
                                            moment.setStatus(MomentStatus.NET);
                                            moment.setAvatar(ppFromString(s, "data.timeline." + i + "._info._creator.head").getAsString());

                                            Pic pic = new Pic();
                                            pic.setKey(ppFromString(s, "data.timeline." + i + "._info.pics.0").getAsString());
                                            pic.setStatus(PicStatus.NET);
                                            moment.setPic(pic);

                                            realm.insertOrUpdate(moment);
                                        }

                                        realm.commitTransaction();
                                    } catch (Exception e) {
                                        realm.cancelTransaction();
                                        throw new Exception(e.toString());
                                    }

                                    if (dataSize < DASHBOARD_MOMENT_PAGE_SIZE) {
                                        //do nothing
                                    } else {
                                        getLatestMoments();
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

    //resize bitmap
    public static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    public static void uploadMoment(final String momentCreatingKey) {
        final byte[] imageData;
        String address;
        String geo;
        String content;
        long createTime;

        try (Realm realm = Realm.getDefaultInstance()) {
            MomentCreating momentCreating = realm.where(MomentCreating.class).equalTo("key", momentCreatingKey).findFirst();

            imageData = momentCreating.getPic();
            address = momentCreating.getAddress();
            geo = momentCreating.getGeo();
            content = momentCreating.getContent();
            createTime = momentCreating.getCreateTime();
        }

        //申请上传图片的token
        final String key = momentCreatingKey + "_0";
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("type", "public")
                .put("filename", key);

        final Observable<String> requestToken = PPRetrofit.getInstance().api("system.generateUploadToken", jBody.getJSONObject());

        //上传moment
        JSONArray jsonArrayPics = new JSONArray();

        jsonArrayPics.put(key);

        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("pics", jsonArrayPics)
                .put("address", address)
                .put("geo", geo)
                .put("content", content)
                .put("createTime", createTime);

        final Observable<String> apiResult1 = PPRetrofit.getInstance()
                .api("moment.publish", jBody1.getJSONObject());

        requestToken
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(
                        new Function<String, ObservableSource<String>>() {
                            @Override
                            public ObservableSource<String> apply(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg + ":" + key);
                                }
                                String token = ppFromString(s, "data.token").getAsString();
                                return uploadSingleImage(imageData, key, token);
                            }
                        }
                )
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull String s) throws Exception {
                        return apiResult1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg);
                                }

                                String uploadedMomentId = ppFromString(s, "data.id").getAsString();

                                try (Realm realm = Realm.getDefaultInstance()) {
                                    MomentCreating momentCreating = realm.where(MomentCreating.class).equalTo("key", momentCreatingKey).findFirst();

                                    realm.beginTransaction();
                                    //删除momentCreating
                                    momentCreating.deleteFromRealm();

                                    realm.commitTransaction();

                                    //通知更新本地Moment中对应moment
                                    EventBus.getDefault().post(new MomentPublishEvent(uploadedMomentId));
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    MomentCreating momentCreating = realm.where(MomentCreating.class).equalTo("key", momentCreatingKey).findFirst();
                                    Moment moment = realm.where(Moment.class).equalTo("key", momentCreatingKey).findFirst();

                                    realm.beginTransaction();
                                    //修改momentCreating放入Moment状态
                                    momentCreating.setStatus(MomentStatus.FAILED);
                                    moment.setStatus(MomentStatus.FAILED);

                                    realm.commitTransaction();
                                }
                                error(throwable.toString());
                            }
                        }
                );
    }

    public static void refreshMoment(String id) {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("id", id);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.detail", jBody.getJSONObject());

        apiResult.subscribe(
                new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        long createTime = ppFromString(s, "data.createTime").getAsLong();

                        try (Realm realm = Realm.getDefaultInstance()) {
                            try {
                                realm.beginTransaction();

                                Moment moment = new Moment();
                                moment.setKey(createTime + "_" + ppFromString(s, "data._creator.id").getAsString());
                                moment.setId(ppFromString(s, "data._id").getAsString());
                                moment.setUserId(ppFromString(s, "data._creator.id").getAsString());
                                moment.setCreateTime(createTime);
                                moment.setStatus(MomentStatus.NET);
                                moment.setAvatar(ppFromString(s, "data._creator.head").getAsString());

                                Pic pic = new Pic();
                                pic.setKey(ppFromString(s, "data.pics.0").getAsString());
                                pic.setStatus(PicStatus.NET);
                                moment.setPic(pic);

                                realm.insertOrUpdate(moment);

                                realm.commitTransaction();
                            } catch (Exception e) {
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

    public static void getServerMomentDetail(final String momentId) {
        //请求服务器最新MomentDetail记录
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("id", momentId)
                .put("checkFollow", "1")
                .put("checkLike", "1");

        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("moment.detail", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("id", momentId)
                .put("beforeTime", "")
                .put("afterTime", "1");

        final Observable<String> apiResult2 = PPRetrofit.getInstance().api("moment.getReplies", jBody2.getJSONObject());

        Observable
                .zip(
                        apiResult1, apiResult2, new BiFunction<String, String, String[]>() {

                            @Override
                            public String[] apply(@NonNull String s, @NonNull String s2) throws Exception {
                                String[] result = {s, s2};

                                return result;
                            }
                        }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String[]>() {
                            @Override
                            public void accept(@NonNull String[] result) throws Exception {
                                //更新本地最新MomentDetail记录到最新MomentDetail记录
                                PPWarn ppWarn = ppWarning(result[0]);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPWarn ppWarn2 = ppWarning(result[1]);

                                if (ppWarn2 != null) {
                                    throw new Exception(ppWarn2.msg);
                                }

                                processMomentDetailAndComments(result[0], result[1]);
                            }
                        }
                        ,
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                            }
                        }
                );
    }

    private static void processMomentDetailAndComments(String momentDetailString, String commentsString) {
        //构造comment detail和comments
        long now = System.currentTimeMillis();

        String momentId = ppFromString(momentDetailString, "data._id").getAsString();

        MomentDetail momentDetail = new MomentDetail();
        momentDetail.setId(momentId);
        momentDetail.setGeo(ppFromString(momentDetailString, "data.location.geo.0").getAsString() + "," + ppFromString(momentDetailString, "data.location.geo.1").getAsString());
        momentDetail.setAddress(ppFromString(momentDetailString, "data.location.detail").getAsString());
        momentDetail.setCity(ppFromString(momentDetailString, "data.location.city").getAsString());
        momentDetail.setUserId(ppFromString(momentDetailString, "data._creator.id").getAsString());
        momentDetail.setContent(ppFromString(momentDetailString, "data.content").getAsString());
        momentDetail.setLiked(ppFromString(momentDetailString, "data.isLiked").getAsInt() == 1 ? true : false);
        momentDetail.setCreateTime(ppFromString(momentDetailString, "data.createTime").getAsLong());
        momentDetail.setPic(ppFromString(momentDetailString, "data.pics.0").getAsString());
        momentDetail.setAvatar(ppFromString(momentDetailString, "data._creator.head").getAsString());
        momentDetail.setNickname(ppFromString(momentDetailString, "data._creator.nickname").getAsString());
        momentDetail.setLastVisitTime(now);

        JsonArray ja = ppFromString(commentsString, "data.list").getAsJsonArray();

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            try {
                for (int i = 0; i < ja.size(); i++) {

                    long createTime = ppFromString(commentsString, "data.list." + i + ".createTime").getAsLong();
                    String userId = ppFromString(commentsString, "data.list." + i + "._creator.id").getAsString();

                    Comment comment = realm.where(Comment.class).equalTo("key", createTime + "_" + userId).equalTo("deleted", true).findFirst();
                    if (comment != null) {
                        //如果这条记录本地已经标志为deleted, 则跳过
                        continue;
                    }

                    comment = new Comment();

                    comment.setKey(createTime + "_" + userId);
                    comment.setId(ppFromString(commentsString, "data.list." + i + "._id").getAsString());
                    comment.setUserId(ppFromString(commentsString, "data.list." + i + "._creator.id").getAsString());
                    comment.setMomentId(momentId);
                    comment.setContent(ppFromString(commentsString, "data.list." + i + ".content").getAsString());
                    comment.setCreateTime(createTime);
                    comment.setNickname(ppFromString(commentsString, "data.list." + i + "._creator.nickname").getAsString());
                    comment.setAvatar(ppFromString(commentsString, "data.list." + i + "._creator.head").getAsString());
                    comment.setStatus(CommentStatus.NET);
                    comment.setReferUserId(ppFromString(commentsString, "data.list." + i + ".refer.id", PPValueType.STRING).getAsString());
                    comment.setReferNickname(ppFromString(commentsString, "data.list." + i + ".refer.nickname", PPValueType.STRING).getAsString());
                    comment.setBePrivate(ppFromString(commentsString, "data.list." + i + ".isPrivate").getAsBoolean());
                    comment.setLastVisitTime(now);
                    realm.insertOrUpdate(comment);
                }

                //把服务器上已删除的comment从本地删掉
                realm.where(Comment.class).equalTo("momentId", momentId).notEqualTo("lastVisitTime", now).findAll().deleteAllFromRealm();

                realm.insertOrUpdate(momentDetail);

                realm.commitTransaction();
            } catch (Exception e) {
                error(e.toString());
                realm.cancelTransaction();
            }
        }
    }

    public static void removeComment(final String commentId, final String momentId) {

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("replyID", commentId)
                .put("momentID", momentId);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.delReply", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {

                                PPWarn ppWarn = ppWarning(s);

                                //pptodo 如果是已被删除错误提示也可以认为成功
                                if (ppWarn != null && ppWarn.code != 1001) {
                                    throw new Exception(ppWarn.msg);
                                }

                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.beginTransaction();
                                    //这里用findAll().deleteAllFromRealm()就不用考虑多线程的问题, 最多查出来结果是空的list, 然后调用deleteAllFromRealm(), 应该不会出错
                                    realm1.where(Comment.class).equalTo("id", commentId).findAll().deleteAllFromRealm();
                                    realm1.commitTransaction();
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                                //pptodo 如果是断网状况就不用重试了, 在重现连上网的时候重新删除即可
                                removeComment(commentId, momentId);
                            }
                        }
                );
    }

    public static void removeMoment(final String momentId) {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("id", momentId);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.del", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {

                                PPWarn ppWarn = ppWarning(s);

                                //pptodo 如果是已被删除错误提示也可以认为成功
                                if (ppWarn != null && ppWarn.code != 1001) {
                                    throw new Exception(ppWarn.msg);
                                }

                                try (Realm realm = Realm.getDefaultInstance()) {
                                    realm.beginTransaction();
                                    //这里用findAll().deleteAllFromRealm()就不用考虑多线程的问题, 最多查出来结果是空的list, 然后调用deleteAllFromRealm(), 应该不会出错
                                    realm.where(Moment.class).equalTo("id", momentId).findAll().deleteAllFromRealm();
                                    realm.commitTransaction();
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                                //pptodo 如果是断网状况就不用重试了, 在重现连上网的时候重新删除即可
                                removeMoment(momentId);
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

    public static Observable<String> uploadSingleImage(final byte[] data, final String key, final String token) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                uploadManager.put(data, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                if (info.isOK()) {
                                    emitter.onNext(key);
                                    emitter.onComplete();
                                } else {
                                    Log.i("qiniu", "Upload Fail");
                                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                                    Exception apiError = new Exception("七牛上传:" + key + "失败", new Throwable(info.error.toString()));
                                    emitter.onError(apiError);
                                }
                            }
                        }, null);
            }
        });
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
                                MessageUnpacker unPacker = MessagePack.newDefaultUnpacker((byte[]) args[0]);

                                try {
                                    String msg = unPacker.unpackValue().toString();
                                    JSONArray jsonArray = new JSONArray(msg);
                                    Log.v("ppLog", "sync from server:" + msg);

                                    if (jsonArray.get(0).toString().equals("moment_reply")) {
                                        JSONObject body = jsonArray.getJSONObject(1);
                                        String momentId = body.getString("mid");
                                        getServerMomentDetail(momentId);
                                    } else {
                                        getSocketPush();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.v("ppLog", "sync from server err:");
                                }

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

    public static void stopSocket() {
        if (socket != null && socket.connected()) {
            socket.close();
        }

        socket = null;
    }
}
