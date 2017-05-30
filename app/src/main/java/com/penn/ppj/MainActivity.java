package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.penn.ppj.databinding.ActivityMainBinding;
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

import static com.penn.ppj.R.id.fab;

public class MainActivity extends AppCompatActivity {
    //变量
    private ActivityMainBinding binding;
    private Realm realm;
    private CurrentUser currentUser;

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
        setupForLoginUser();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void UserLogoutEvent(UserLogoutEvent event) {
        setupForGuest();
    }

    //帮助函数
    public void showDb(View v) {
        Realm realm = Realm.getDefaultInstance();
        RealmConfiguration configuration = realm.getConfiguration();
        realm.close();
        RealmBrowser.startRealmModelsActivity(this, configuration);
    }

    private void setup() {
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
        NearbyFragment nearbyFragment = new NearbyFragment();
        adapter.addFragment(nearbyFragment, "C1");

        binding.viewPager.setAdapter(adapter);

        //有几个tab就设几防止page自己重新刷新
        binding.viewPager.setOffscreenPageLimit(3);

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
            setupForLoginUser();
            refreshRelatedUsers();
        } else {
            setupForGuest();
        }
    }

    private void setupForLoginUser() {
        //开启socket
        try {
            PPApplication.startSocket();
        } catch (Exception e) {
            PPApplication.error(e.toString());
        }

        //初始化realm
        PPApplication.initRealm(PPApplication.getPrefStringValue(PPApplication.CUR_USER_ID, ""));

        //初始化currentUser
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

    //refresh RelatedUsers
    private void refreshRelatedUsers() {
        PPApplication.refreshRelatedUsers()
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


    private void setupForGuest() {
        binding.setData(null);
    }
}
