package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.ppj.databinding.FragmentCommentInputBottomSheetDialogBinding;
import com.penn.ppj.databinding.FragmentRelatedUserBottomSheetDialogBinding;
import com.penn.ppj.databinding.RelatedUserCellBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.RelatedUserType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class RelatedUserBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private FragmentRelatedUserBottomSheetDialogBinding binding;
    private RelatedUserType relatedUserType;
    private Realm realm;
    private RealmResults<RelatedUser> relatedUsers;
    private PPAdapter ppAdapter;
    private LinearLayoutManager linearLayoutManager;

    private class PPAdapter extends RecyclerView.Adapter<PPAdapter.PPViewHolder> {
        private List<RelatedUser> data;

        public class PPViewHolder extends RecyclerView.ViewHolder {
            private RelatedUserCellBinding binding;

            public PPViewHolder(RelatedUserCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<RelatedUser> data) {
            this.data = data;
        }

        @Override
        public PPViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            RelatedUserCellBinding relatedUserCellBinding = RelatedUserCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            relatedUserCellBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = binding.mainRecyclerView.getChildAdapterPosition(v);
                    RelatedUser relatedUser = relatedUsers.get(position);
                    Intent intent = new Intent(getContext(), UserHomePageActivity.class);
                    intent.putExtra("userId", relatedUser.getUserId());
                    startActivity(intent);
                }
            });

            return new PPViewHolder(relatedUserCellBinding);

        }

        @Override
        public void onBindViewHolder(PPViewHolder holder, int position) {

            holder.binding.setData(relatedUsers.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    public static RelatedUserBottomSheetDialogFragment newInstance(RelatedUserType relatedUserType) {
        RelatedUserBottomSheetDialogFragment fragment = new RelatedUserBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString("relatedUserType", relatedUserType.toString());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_related_user_bottom_sheet_dialog, container, false);

        setup();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        realm.close();
        super.onDestroyView();
    }

    private void setup() {
        realm = Realm.getDefaultInstance();

        relatedUserType = RelatedUserType.valueOf(getArguments().getString("relatedUserType"));

        int tmpId = 0;
        if (relatedUserType == RelatedUserType.FOLLOW) {
            tmpId = R.string.follow;
        } else if (relatedUserType == RelatedUserType.FAN) {
            tmpId = R.string.fan;
        } else if (relatedUserType == RelatedUserType.FRIEND) {
            tmpId = R.string.friend;
        }

        binding.titleTextView.setText(getString(tmpId));

        relatedUsers = realm.where(RelatedUser.class).equalTo("type", relatedUserType.toString()).findAllSorted("createTime", Sort.DESCENDING);
        relatedUsers.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RelatedUser>>() {
            @Override
            public void onChange(RealmResults<RelatedUser> collection, OrderedCollectionChangeSet changeSet) {
                // `null`  means the async query returns the first time.
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

        ppAdapter = new PPAdapter(relatedUsers);
        binding.mainRecyclerView.setAdapter(ppAdapter);

        linearLayoutManager = new LinearLayoutManager(getContext());
        binding.mainRecyclerView.setLayoutManager(linearLayoutManager);
    }
}
