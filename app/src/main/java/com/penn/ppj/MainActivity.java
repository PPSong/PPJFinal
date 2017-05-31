package com.penn.ppj;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.penn.ppj.databinding.ActivityMainBinding;
import com.penn.ppj.messageEvent.MomentCreatedEvent;
import com.penn.ppj.messageEvent.MomentDeleteEvent;
import com.penn.ppj.messageEvent.MomentPublishEvent;
import com.penn.ppj.messageEvent.NotificationEvent;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.util.PPPagerAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.jonasrottmann.realmbrowser.RealmBrowser;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.ObjectChangeSet;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmObjectChangeListener;

public class MainActivity extends AppCompatActivity {
    //变量
    private static final int PP_ACCESS_COARSE_LOCATION = 1000;

    private ActivityMainBinding binding;
    private Realm realm;
    private CurrentUser currentUser;
    private DashboardFragment dashboardFragment;

    //构造函数

    //本类覆盖函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setup();

        //注册event bus
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        //注销event but
        EventBus.getDefault().unregister(this);

        //关闭realm
        if (realm != null && !realm.isClosed()) {
            realm.close();
        }
        super.onDestroy();
    }

    //接口覆盖函数

    //EventBus
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void UserLoginEvent(UserLoginEvent event) {
        onLogin();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void UserLogoutEvent(UserLogoutEvent event) {
        onLogout();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void NotificationEvent(NotificationEvent event) {
        setBadge(event.num);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MomentCreatedEvent(MomentCreatedEvent event) {
        binding.viewPager.setCurrentItem(1);
        dashboardFragment.binding.recyclerView.scrollToPosition(0);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void MomentDeleteEvent(MomentDeleteEvent event) {
        PPApplication.removeMoment(event.id);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void MomentPublishEvent(MomentPublishEvent event) {
        PPApplication.refreshMoment(event.id);
    }

    //帮助函数
    public void showDb(View v) {
        Realm realm = Realm.getDefaultInstance();
        RealmConfiguration configuration = realm.getConfiguration();
        realm.close();
        RealmBrowser.startRealmModelsActivity(this, configuration);
    }

    //设置消息未读数
    private void setBadge(int num) {
        if (num > 0) {
            binding.badge.setVisibility(View.VISIBLE);
            binding.badge.setText("" + num);
        } else {
            binding.badge.setVisibility(View.INVISIBLE);
        }
    }

    private void setup() {
        requestPermission(this);

        //toolbar
        setSupportActionBar(binding.toolbar);
        //设置toolbar高度和padding
        AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) binding.toolbar.getLayoutParams();
        layoutParams.height = PPApplication.getStatusBarAddActionBarHeight();
        binding.toolbar.setLayoutParams(layoutParams);
        binding.toolbar.setPadding(0, PPApplication.getStatusBarHeight(), 0, 0);

        //avatar
        binding.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PPApplication.isLogin()) {
                    PPApplication.logout();
                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        //view pager
        PPPagerAdapter adapter = new PPPagerAdapter(getSupportFragmentManager());
        NearbyFragment nearbyFragment = NearbyFragment.newInstance();
        dashboardFragment = DashboardFragment.newInstance();
        NotificationFragment notificationFragment = NotificationFragment.newInstance();
        adapter.addFragment(nearbyFragment, "C1");
        adapter.addFragment(dashboardFragment, "C2");
        adapter.addFragment(notificationFragment, "C3");

        binding.viewPager.setAdapter(adapter);

        //有几个tab就设几防止page自己重新刷新
        binding.viewPager.setOffscreenPageLimit(3);

        //设置导航按钮
        binding.goNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.viewPager.setCurrentItem(0, true);
            }
        });

        binding.goDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.viewPager.setCurrentItem(1, true);
            }
        });

        binding.goNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.viewPager.setCurrentItem(2, true);
            }
        });

        //fab
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PPApplication.isLogin()) {
                    Intent intent = new Intent(MainActivity.this, MomentCreatingActivity.class);
                    startActivity(intent);
                } else {
                    PPApplication.goLogin(MainActivity.this);
                }

            }
        });

        binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (PPApplication.isLogin()) {
            onLogin();
            //更新相关用户
            PPApplication.doRefreshRelatedUsers();
        }
    }

    private void onLogin() {
        //开启socket
        try {
            PPApplication.startSocket();
        } catch (Exception e) {
            PPApplication.error(e.toString());
        }

        //初始化realm
        PPApplication.initRealm(PPApplication.getCurrentUserId());
        realm = Realm.getDefaultInstance();

        //先取得本地数据库中的CurrentUser
        currentUser = realm.where(CurrentUser.class).findFirst();
        binding.setData(currentUser);

        //设置CurrentUser动态更新
        currentUser.addChangeListener(new RealmObjectChangeListener<RealmModel>() {
            @Override
            public void onChange(RealmModel object, ObjectChangeSet changeSet) {
                binding.setData(currentUser);
            }
        });

        //取得最新Current
        PPApplication.refreshCurrentUser();
    }

    private void onLogout() {
        //关闭socket
        PPApplication.stopSocket();

        //关闭realm
        realm.close();

        //清空binding data
        binding.setData(null);

        //设置CurrentUser动态更新
        currentUser.removeAllChangeListeners();
    }

    private void requestPermission(Activity activity) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PP_ACCESS_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PP_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    PPApplication.error(getString(R.string.need_permission));
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
