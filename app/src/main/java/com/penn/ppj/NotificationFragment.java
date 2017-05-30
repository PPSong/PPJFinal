package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.ppj.databinding.FragmentDashboardBinding;
import com.penn.ppj.databinding.FragmentNotificationBinding;
import com.penn.ppj.databinding.MessageCellBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.messageEvent.NotificationEvent;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.model.realm.Message;
import com.penn.ppj.model.realm.Moment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class NotificationFragment extends Fragment {
    //变量
    private FragmentNotificationBinding binding;
    private Realm realm;
    private RealmResults<Message> messages;
    private RealmResults<Message> unReadMessages;
    private PPAdapter ppAdapter;
    private LinearLayoutManager linearLayoutManager;

    private class PPAdapter extends RecyclerView.Adapter<PPAdapter.PPHoldView> {
        private List<Message> data;

        public class PPHoldView extends RecyclerView.ViewHolder {
            private MessageCellBinding binding;

            public PPHoldView(MessageCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<Message> data) {
            this.data = data;
        }

        @Override
        public PPAdapter.PPHoldView onCreateViewHolder(ViewGroup parent, int viewType) {
            MessageCellBinding messageCellBinding = MessageCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            messageCellBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = binding.recyclerView.getChildAdapterPosition(v);
                    Message message = data.get(position);
                    //markLocalMessageRead
                    try (Realm realm = Realm.getDefaultInstance()) {
                        realm.beginTransaction();

                        Message tmpMessage = realm.where(Message.class).equalTo("id", message.getId()).findFirst();
                        tmpMessage.setRead(true);

                        realm.commitTransaction();
                    }
                }
            });

            return new PPHoldView(messageCellBinding);
        }

        @Override
        public void onBindViewHolder(PPAdapter.PPHoldView holder, int position) {
            //设置Message binding数据
            holder.binding.setData(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    //构造函数
    public NotificationFragment() {
        // Required empty public constructor
    }

    public static NotificationFragment newInstance() {
        NotificationFragment fragment = new NotificationFragment();

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notification, container, false);

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
        onLogin();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void UserLogoutEvent(UserLogoutEvent event) {
        onLogout();
    }

    //帮助函数
    private void onLogout() {
        //清空moments
        messages.removeAllChangeListeners();
        messages = null;

        unReadMessages.removeAllChangeListeners();
        unReadMessages = null;

        //通知设置未读消息数0
        EventBus.getDefault().post(new NotificationEvent(0));

        //清空ppAdapter
        ppAdapter = null;
        //清空gridLayoutManager
        linearLayoutManager = null;

        binding.recyclerView.setAdapter(null);
        binding.recyclerView.setLayoutManager(null);

        //关闭realm
        realm.close();
    }

    private void onLogin() {
        //初始化realm
        realm = Realm.getDefaultInstance();

        //先取得本地数据库中的moments
        messages = realm.where(Message.class).findAllSorted("createTime", Sort.DESCENDING);
        //设置moments动态更新
        messages.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<Message>>() {
            @Override
            public void onChange(RealmResults<Message> collection, OrderedCollectionChangeSet changeSet) {
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

        unReadMessages = realm.where(Message.class).equalTo("read", false).findAllSorted("createTime", Sort.DESCENDING);
        //通知设置未读消息数
        EventBus.getDefault().post(new NotificationEvent(unReadMessages.size()));
        //设置unReadMessages动态更新
        unReadMessages.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<Message>>() {
            @Override
            public void onChange(RealmResults<Message> collection, OrderedCollectionChangeSet changeSet) {
                //通知修改未读消息数
                EventBus.getDefault().post(new NotificationEvent(unReadMessages.size()));
            }
        });

        //ppAdapter
        ppAdapter = new PPAdapter(messages);
        //linearLayoutManager
        linearLayoutManager = new LinearLayoutManager(getContext());
        binding.recyclerView.setAdapter(ppAdapter);
        binding.recyclerView.setLayoutManager(linearLayoutManager);

        //取得最新messages
        PPApplication.getLatestMessages();
    }

    private void setup() {
        if (PPApplication.isLogin()) {
            onLogin();
        }
    }
}
