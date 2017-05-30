package com.penn.ppj;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.penn.ppj.databinding.FragmentNearbyBinding;
import com.penn.ppj.databinding.NearbyMomentOverviewCellBinding;
import com.penn.ppj.model.realm.NearbyMoment;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPLoadController;
import com.penn.ppj.util.PPLoadDataAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

public class NearbyFragment extends Fragment {
    //变量
    public static final int NEARBY_MOMENT_PAGE_SIZE = 10;

    private FragmentNearbyBinding binding;
    private RealmConfiguration realmDefaultConfig = new RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .name("ppDefault.realm")
            .build();
    private Realm realm;
    private RealmResults<NearbyMoment> nearbyMoments;
    private PPAdapter ppAdapter;
    private GridLayoutManager gridLayoutManager;
    private PPLoadController ppLoadController;
    private long earliestCreateTime;
    private String geoStr = PPApplication.getLatestGeoString();

    private class PPAdapter extends PPLoadDataAdapter<NearbyMoment, NearbyMoment> {

        public PPAdapter(List<NearbyMoment> data) {
            this.data = data;
        }

        public class PPViewHolder extends RecyclerView.ViewHolder {
            private NearbyMomentOverviewCellBinding binding;

            public PPViewHolder(NearbyMomentOverviewCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        @Override
        protected RecyclerView.ViewHolder ppOnCreateViewHolder(ViewGroup parent, int viewType) {
            NearbyMomentOverviewCellBinding nearbyMomentOverviewCellBinding = NearbyMomentOverviewCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            //设置cell被点击事件
            nearbyMomentOverviewCellBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PPApplication.isLogin()) {
                        int position = binding.recyclerView.getChildAdapterPosition(v);
                        NearbyMoment nearbyMoment = data.get(position);
//                        Intent intent = new Intent(getContext(), MomentDetailActivity.class);
//                        intent.putExtra("momentId", nearbyMoment.getId());
//                        startActivity(intent);
                    } else {
                        PPApplication.goLogin(getActivity());
                    }
                }
            });

            return new PPViewHolder(nearbyMomentOverviewCellBinding);
        }

        @Override
        protected int ppGetItemViewType(int position) {
            return 0;
        }

        @Override
        public void ppOnBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            //清空当前图片
            ((PPViewHolder) holder).binding.mainImageView.setImageResource(0);
            //设置图片背景色
            ((PPViewHolder) holder).binding.mainImageView.setBackgroundColor(PPApplication.getMomentOverviewBackgroundColor(position));
            //设置NearbyMoment binding数据
            ((PPViewHolder) holder).binding.setData(data.get(position));
        }

        @Override
        protected void addLoadMoreData(final List data) {
            //因为用了realm, 不需要做什么
        }

        @Override
        protected void addRefreshData(List data) {
            //因为用了realm, 不需要做什么
        }
    }

    //构造函数
    public NearbyFragment() {
        // Required empty public constructor
    }

    public static NearbyFragment newInstance() {
        NearbyFragment fragment = new NearbyFragment();

        return fragment;
    }

    //本类覆盖函数
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_nearby, container, false);

        setup();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        realm.close();
        super.onDestroyView();
    }

    //接口覆盖函数

    //帮助函数
    private void setup() {
        //使用匿名用户默认realm数据库
        realm = Realm.getInstance(realmDefaultConfig);

        //取得nearbyMoments并设置变化监听
        nearbyMoments = realm.where(NearbyMoment.class).findAllSorted("createTime", Sort.DESCENDING);
        nearbyMoments.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<NearbyMoment>>() {
            @Override
            public void onChange(RealmResults<NearbyMoment> collection, OrderedCollectionChangeSet changeSet) {
                Log.v("pplog", "changed");
                //ppAdapter.notifyDataSetChanged();
                if (changeSet == null) {
                    ppAdapter.notifyDataSetChanged();
                    return;
                }
                // For deletions, the adapter has to be notified in reverse order.
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    Log.v("pplog", "delete length:" + i + ":" + deletions[i].length);
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    ppAdapter.notifyItemRangeRemoved(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    Log.v("pplog", "insert length:" + range.length);
                    ppAdapter.notifyItemRangeInserted(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    Log.v("pplog", "change length:" + range.length);
                    ppAdapter.notifyItemRangeChanged(range.startIndex, range.length);
                }
            }
        });

        //ppAdapter
        ppAdapter = new PPAdapter(nearbyMoments);

        //gridLayoutManager
        final int spanNum = PPApplication.calculateNoOfColumns();
        gridLayoutManager = new GridLayoutManager(getContext(), spanNum);
        gridLayoutManager.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        if (ppAdapter.getItemViewType(position) == PPLoadDataAdapter.LOAD_MORE_SPIN
                                || ppAdapter.getItemViewType(position) == PPLoadDataAdapter.NO_MORE) {
                            return spanNum;
                        } else {
                            return 1;
                        }
                    }
                }
        );

        //ppLoadController
        ppLoadController = new PPLoadController(binding.swipeRefreshLayout, binding.recyclerView, ppAdapter, gridLayoutManager, new PPLoadController.LoadDataProvider() {
            @Override
            public void refreshData() {
                geoStr = PPApplication.getLatestGeoString();
                earliestCreateTime = Long.MAX_VALUE;

                PPJSONObject jBody = new PPJSONObject();
                jBody
                        .put("geo", PPApplication.getLatestGeoString())
                        .put("beforeTime", "" + Long.MAX_VALUE);

                final Observable<String> apiResult = PPRetrofit.getInstance().api("moment.search", jBody.getJSONObject());

                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                ppLoadController.endRefreshSpinner();
                            }
                        })
                        .observeOn(Schedulers.io())
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(@NonNull String s) throws Exception {
                                        PPWarn ppWarn = PPApplication.ppWarning(s);

                                        if (ppWarn != null) {
                                            throw new Exception(ppWarn.msg);
                                        }

                                        //判断是否noMore
                                        JsonArray ja = PPApplication.ppFromString(s, "data.list").getAsJsonArray();
                                        int size = ja.size();
                                        ppLoadController.ppLoadDataAdapter.getRefreshData(nearbyMoments, size < NEARBY_MOMENT_PAGE_SIZE);

                                        //解析并插入获得的nearbyMoments
                                        processMoment(s, true);
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

            @Override
            public void loadMoreData() {
                PPJSONObject jBody = new PPJSONObject();
                jBody
                        .put("geo", geoStr)
                        .put("beforeTime", "" + earliestCreateTime);

                final Observable<String> apiResult = PPRetrofit.getInstance().api("moment.search", jBody.getJSONObject());

                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                ppLoadController.removeLoadMoreSpinner();
                            }
                        })
                        .observeOn(Schedulers.io())
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(@NonNull String s) throws Exception {
                                        PPWarn ppWarn = PPApplication.ppWarning(s);

                                        if (ppWarn != null) {
                                            throw new Exception(ppWarn.msg);
                                        }

                                        //判断是否noMore
                                        JsonArray ja = PPApplication.ppFromString(s, "data.list").getAsJsonArray();
                                        int size = ja.size();

                                        ppLoadController.ppLoadDataAdapter.loadMoreEnd(nearbyMoments, size < NEARBY_MOMENT_PAGE_SIZE);

                                        //解析并插入获得的nearbyMoments
                                        processMoment(s, false);
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
        });
    }

    //解析得到的nearbyMoments
    private void processMoment(String s, boolean refresh) {
        try (Realm realm = Realm.getInstance(realmDefaultConfig)) {
            realm.beginTransaction();

            try {
                if (refresh) {
                    realm.where(NearbyMoment.class).findAll().deleteAllFromRealm();
                }

                JsonArray ja = PPApplication.ppFromString(s, "data.list").getAsJsonArray();

                int size = ja.size();

                for (int i = 0; i < size; i++) {
                    long createTime = PPApplication.ppFromString(s, "data.list." + i + ".createTime").getAsLong();

                    //注意,由于api会返回重复的数据,如果用以下排重的做法的话,会导致闪屏,因为一次加载不能满足一屏,又会触发load more
                    NearbyMoment nearbyMoment = realm.where(NearbyMoment.class).equalTo("key", createTime + "_" + PPApplication.ppFromString(s, "data.list." + i + "._creator.id").getAsString()).findFirst();
                    if (nearbyMoment == null) {
                        nearbyMoment = new NearbyMoment();
                        nearbyMoment.setKey(createTime + "_" + PPApplication.ppFromString(s, "data.list." + i + "._creator.id").getAsString());
                        nearbyMoment.setId(PPApplication.ppFromString(s, "data.list." + i + "._id").getAsString());
                        nearbyMoment.setCreateTime(createTime);
                        nearbyMoment.setPic(PPApplication.ppFromString(s, "data.list." + i + ".pics.0").getAsString());

                        realm.insert(nearbyMoment);

                        if (earliestCreateTime > createTime) {
                            //更新最老记录时间戳
                            earliestCreateTime = createTime;
                        }
                    } else {
                        //do nothing
                    }
                }

                realm.commitTransaction();
            } catch (Exception e) {
                PPApplication.error(e.toString());
                realm.cancelTransaction();
            }
        }
    }
}
