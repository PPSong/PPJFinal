package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.databinding.ActivityUserHomePageBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.UserHomePage;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPPagerAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import io.realm.ObjectChangeSet;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmObjectChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.penn.ppj.PPApplication.ppFromString;
import static com.penn.ppj.PPApplication.ppWarning;

public class UserHomePageActivity extends AppCompatActivity {
    //变量
    private ActivityUserHomePageBinding binding;

    private Realm realm;
    private String userId;
    private UserHomePage userHomePage;
    private RealmResults<Moment> moments;
    private PPAdapter ppAdapter;
    private GridLayoutManager gridLayoutManager;
    private BottomSheetBehavior mBottomSheetBehavior;

    private class PPAdapter extends RecyclerView.Adapter<PPAdapter.PPViewHolder> {
        private List<Moment> data;

        public class PPViewHolder extends RecyclerView.ViewHolder {
            private MomentOverviewCellBinding binding;

            public PPViewHolder(MomentOverviewCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<Moment> data) {
            this.data = data;
        }

        @Override
        public PPViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            MomentOverviewCellBinding momentOverviewCellBinding = MomentOverviewCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            momentOverviewCellBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = binding.mainRecyclerView.getChildAdapterPosition(v);
                    Moment moment = moments.get(position);
                    Intent intent = new Intent(UserHomePageActivity.this, MomentDetailActivity.class);
                    intent.putExtra("momentId", moment.getId());
                    startActivity(intent);
                }
            });

            return new PPViewHolder(momentOverviewCellBinding);
        }

        @Override
        public void onBindViewHolder(PPViewHolder holder, int position) {

            holder.binding.setData(moments.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }


    //构造函数

    //本类覆盖函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_home_page);

        setup();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    //接口覆盖函数

    //帮助函数
    private void setup() {
        binding.followToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TransitionManager.beginDelayedTransition(binding.mainLinearLayout);
            }
        });

        //like按钮监控
        RxView.clicks(binding.followToggleButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                boolean followed = binding.followToggleButton.isChecked();
                                Log.v("pplog", "follow2:" + followed);
                                followOrUnfollowMoment(followed);
                            }
                        }
                );

        //back按钮监控
        Observable<Object> backButtonObservable = RxView.clicks(binding.backImageButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        backButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                finish();
                            }
                        }
                );

        realm = Realm.getDefaultInstance();

        userId = getIntent().getStringExtra("userId");

        userHomePage = realm.where(UserHomePage.class).equalTo("userId", userId).findFirst();

        if (userHomePage != null) {
            firstSetBinding(userHomePage);

        }

        getServerUserHomePage(userId);

        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet1);

        mBottomSheetBehavior.setHideable(false);

        TypedValue tv = new TypedValue();
//        int actionBarHeight = 0;
//        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
//        {
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
//        }

        mBottomSheetBehavior.setPeekHeight(PPApplication.calculateMomentOverHeight());
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        moments = realm.where(Moment.class).equalTo("userId", userId).findAllSorted("createTime", Sort.DESCENDING);
        moments.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<Moment>>() {
            @Override
            public void onChange(RealmResults<Moment> collection, OrderedCollectionChangeSet changeSet) {
                if (changeSet == null) {
                    ppAdapter.notifyDataSetChanged();
                    return;
                }
                // For deletions, the adapter has to be notified in reverse order.
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    ppAdapter.notifyItemRangeRemoved(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    ppAdapter.notifyItemRangeInserted(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    ppAdapter.notifyItemRangeChanged(range.startIndex, range.length);
                }
            }
        });

        gridLayoutManager = new GridLayoutManager(this, PPApplication.calculateNoOfColumns());

        binding.mainRecyclerView.setLayoutManager(gridLayoutManager);

        ppAdapter = new PPAdapter(moments);
        binding.mainRecyclerView.setAdapter(ppAdapter);
    }

    private void firstSetBinding(UserHomePage userHomePage) {
        //修改本地记录lastVisitTime
        realm.beginTransaction();
        userHomePage.setLastVisitTime(System.currentTimeMillis());
        realm.commitTransaction();

        userHomePage.addChangeListener(new RealmObjectChangeListener<UserHomePage>() {
            @Override
            public void onChange(UserHomePage object, ObjectChangeSet changeSet) {
                if (object.isValid()) {
                    binding.setData(object);
                }
            }
        });

        //set binding data
        binding.setData(userHomePage);
        setupButtonsText();
    }

    private void followOrUnfollowMoment(boolean follow) {
        String api = follow ? "friend.follow" : "friend.unFollow";

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("target", userId)
                .put("isFree", "true");

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api(api, jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                getServerUserHomePage(userId);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPApplication.error(throwable.toString());
                                restoreLocalUserHomePage();
                            }
                        }
                );
    }

    private void restoreLocalUserHomePage() {
        binding.setData(userHomePage);
    }

    private void getServerUserHomePage(final String userId) {
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("target", userId);

        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("friend.isFollowed", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("target", userId);

        final Observable<String> apiResult2 = PPRetrofit.getInstance().api("user.info", jBody2.getJSONObject());

        PPJSONObject jBody3 = new PPJSONObject();
        jBody3
                .put("target", userId);

        final Observable<String> apiResult3 = PPRetrofit.getInstance().api("user.view", jBody3.getJSONObject());

        Observable
                .zip(
                        apiResult1, apiResult2, apiResult3, new Function3<String, String, String, String[]>() {

                            @Override
                            public String[] apply(@NonNull String s, @NonNull String s2, @NonNull String s3) throws Exception {
                                String[] result = {s, s2, s3};

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
                                //更新本地UserHomePage
                                PPWarn ppWarn = ppWarning(result[0]);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPWarn ppWarn2 = ppWarning(result[1]);

                                if (ppWarn2 != null) {
                                    throw new Exception(ppWarn2.msg);
                                }

                                PPWarn ppWarn3 = ppWarning(result[2]);

                                if (ppWarn3 != null) {
                                    throw new Exception(ppWarn3.msg);
                                }

                                processUserHomePage(result[0], result[1], result[2]);

                                if (userHomePage == null) {
                                    userHomePage = realm.where(UserHomePage.class).equalTo("userId", userId).findFirst();
                                    firstSetBinding(userHomePage);
                                }
                            }
                        }
                        ,
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPApplication.error(throwable.toString());
                            }
                        }
                );
    }

    private void processUserHomePage(String isFollowed, String userInfo, String userView) {
        try (Realm realm = Realm.getDefaultInstance()) {
            try {
                realm.beginTransaction();

                //构造userHomePage
                long now = System.currentTimeMillis();

                UserHomePage userHomePage = new UserHomePage();
                userHomePage.setFollowed(ppFromString(isFollowed, "data.follow").getAsInt() == 0 ? false : true);
                userHomePage.setUserId(ppFromString(userInfo, "data.profile.id").getAsString());
                userHomePage.setNickname(ppFromString(userInfo, "data.profile.nickname").getAsString());
                userHomePage.setAvatar(ppFromString(userInfo, "data.profile.head").getAsString());
                userHomePage.setMeets(ppFromString(userView, "data.meets").getAsInt());
                userHomePage.setCollects(ppFromString(userView, "data.collects").getAsInt());
                userHomePage.setBeCollecteds(ppFromString(userView, "data.beCollecteds").getAsInt());
                userHomePage.setLastVisitTime(now);

                realm.insertOrUpdate(userHomePage);

                realm.commitTransaction();
            } catch (Exception e) {
                PPApplication.error(e.toString());
                realm.cancelTransaction();
            }

        }
    }

    private void setupButtonsText() {
        binding.meetButton.setText("" + userHomePage.getMeets() + getString(R.string.meet));
        binding.collectButton.setText("" + userHomePage.getCollects() + getString(R.string.collect));
        binding.beCollectedButton.setText("" + userHomePage.getBeCollecteds() + getString(R.string.be_collected));
    }
}
