package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.databinding.ActivityMyProfileBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPPagerAdapter;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.realm.ObjectChangeSet;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmObjectChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class MyProfileActivity extends AppCompatActivity {
    //变量
    private ActivityMyProfileBinding binding;
    private Realm realm;
    private CurrentUser currentUser;
    private RealmResults<Moment> moments;
    private RealmResults<RelatedUser> friends;
    private RealmResults<RelatedUser> fans;
    private RealmResults<RelatedUser> follows;

    //构造函数

    //本类覆盖函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_profile);

        setup();
    }

    //接口覆盖函数

    //帮助函数
    private void setup() {
        realm = Realm.getDefaultInstance();

        currentUser = realm.where(CurrentUser.class).findFirst();
        binding.setData(currentUser);
        currentUser.addChangeListener(new RealmObjectChangeListener<CurrentUser>() {
            @Override
            public void onChange(CurrentUser object, ObjectChangeSet changeSet) {
                binding.setData(object);
            }
        });

        friends = realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FRIEND.toString()).findAllSorted("createTime", Sort.DESCENDING);
        friends.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RelatedUser>>() {
            @Override
            public void onChange(RealmResults<RelatedUser> collection, OrderedCollectionChangeSet changeSet) {
                binding.friendsButton.setText(friends.size());
            }
        });

        follows = realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FOLLOW.toString()).findAllSorted("createTime", Sort.DESCENDING);
        follows.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RelatedUser>>() {
            @Override
            public void onChange(RealmResults<RelatedUser> collection, OrderedCollectionChangeSet changeSet) {
                binding.followsButton.setText(follows.size());
            }
        });

        fans = realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FAN.toString()).findAllSorted("createTime", Sort.DESCENDING);
        fans.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RelatedUser>>() {
            @Override
            public void onChange(RealmResults<RelatedUser> collection, OrderedCollectionChangeSet changeSet) {
                binding.fansButton.setText(fans.size());
            }
        });

        //back按钮监控
        RxView.clicks(binding.backImageButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                finish();
                            }
                        }
                );

        //fans按钮监控
        RxView.clicks(binding.fansButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                ((AnimatedVectorDrawable) binding.fansButton.getCompoundDrawables()[1]).start();
                                showRelatedUsers(RelatedUserType.FAN);
                            }
                        }
                );

        //follows按钮监控
        RxView.clicks(binding.followsButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                ((AnimatedVectorDrawable) binding.followsButton.getCompoundDrawables()[1]).start();
                                showRelatedUsers(RelatedUserType.FOLLOW);
                            }
                        }
                );

        //friends按钮监控
        RxView.clicks(binding.friendsButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                ((AnimatedVectorDrawable) binding.friendsButton.getCompoundDrawables()[1]).start();
                                showRelatedUsers(RelatedUserType.FRIEND);
                            }
                        }
                );

        binding.fansButton.setText("" + fans.size() + getString(R.string.fan));
        binding.followsButton.setText("" + follows.size() + getString(R.string.follow));
        binding.friendsButton.setText("" + friends.size() + getString(R.string.friend));
    }

    private void showRelatedUsers(RelatedUserType relatedUserType) {
        RelatedUserBottomSheetDialogFragment relatedUserBottomSheetDialogFragment = RelatedUserBottomSheetDialogFragment.newInstance(relatedUserType);
        relatedUserBottomSheetDialogFragment.show(getSupportFragmentManager(), relatedUserBottomSheetDialogFragment.getTag());
    }
}
