package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.penn.ppj.databinding.FragmentDashboardBinding;
import com.penn.ppj.databinding.FragmentNearbyBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.databinding.NearbyMomentOverviewCellBinding;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.NearbyMoment;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPLoadController;
import com.penn.ppj.util.PPLoadDataAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.ObjectChangeSet;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmObjectChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.penn.ppj.NearbyFragment.NEARBY_MOMENT_PAGE_SIZE;
import static com.penn.ppj.PPApplication.refreshRelatedUsers;

public class DashboardFragment extends Fragment {
    //变量
    private FragmentDashboardBinding binding;
    private Realm realm;
    private RealmResults<Moment> moments;
    private PPAdapter ppAdapter;
    private GridLayoutManager gridLayoutManager;
    private long earliestCreateTime;

    private class PPAdapter extends RecyclerView.Adapter<PPAdapter.PPHoldView> {
        private List<Moment> data;

        public class PPHoldView extends RecyclerView.ViewHolder {
            private MomentOverviewCellBinding binding;

            public PPHoldView(MomentOverviewCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<Moment> data) {
            this.data = data;
        }

        @Override
        public PPAdapter.PPHoldView onCreateViewHolder(ViewGroup parent, int viewType) {
            MomentOverviewCellBinding momentOverviewCellBinding = MomentOverviewCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            momentOverviewCellBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            return new PPHoldView(momentOverviewCellBinding);
        }

        @Override
        public void onBindViewHolder(PPAdapter.PPHoldView holder, int position) {
            //清空当前图片
            holder.binding.mainImageView.setImageResource(0);
            //设置图片背景色
            holder.binding.mainImageView.setBackgroundColor(PPApplication.getMomentOverviewBackgroundColor(position));
            //设置NearbyMoment binding数据
            holder.binding.setData(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    //构造函数
    public DashboardFragment() {
        // Required empty public constructor
    }

    public static DashboardFragment newInstance() {
        DashboardFragment fragment = new DashboardFragment();

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dashboard, container, false);

        setup();

        //注册event bus
        EventBus.getDefault().register(this);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        //注销event but
        EventBus.getDefault().unregister(this);

        //关闭realm
        if (realm != null && !realm.isClosed()) {
            realm.close();
        }

        super.onDestroyView();
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
    private void setupForGuest() {
    }

    private void setupForLoginUser() {
        //初始化realm
        realm = Realm.getDefaultInstance();

        //先取得本地数据库中的moments
        moments = realm.where(Moment.class).notEqualTo("deleted", true).findAllSorted("createTime", Sort.DESCENDING);;
        //设置moments动态更新
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

        //ppAdapter
        ppAdapter = new PPAdapter(moments);
        //gridLayoutManager
        gridLayoutManager = new GridLayoutManager(getContext(), PPApplication.calculateNoOfColumns());
        binding.recyclerView.setAdapter(ppAdapter);
        binding.recyclerView.setLayoutManager(gridLayoutManager);

        //取得最新moments
        PPApplication.getLatestMoments();
    }

    private void setup() {
        if (PPApplication.isLogin()) {
            setupForLoginUser();
        } else {
            setupForGuest();
        }
    }
}
