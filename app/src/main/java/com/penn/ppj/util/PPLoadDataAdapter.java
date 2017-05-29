package com.penn.ppj.util;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.penn.ppj.PPApplication;
import com.penn.ppj.R;

import java.util.List;

import static android.os.Looper.getMainLooper;

/**
 * Created by penn on 11/05/2017.
 */

public abstract class PPLoadDataAdapter<T, S>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int LOAD_MORE_SPIN = -1;
    public static final int NO_MORE = -2;
    private static final long LOAD_MORE_SPIN_ID = Long.MAX_VALUE;
    private static final long NO_MORE_ID = Long.MAX_VALUE - 1;

    private boolean loadMore = false;
    public boolean noMore = false;

    protected List<T> data;

    abstract protected RecyclerView.ViewHolder ppOnCreateViewHolder(ViewGroup parent, int viewType);

    abstract protected int ppGetItemViewType(int position);

    abstract public void ppOnBindViewHolder(RecyclerView.ViewHolder holder, int position);

    abstract protected void addLoadMoreData(List<S> data);

    abstract protected void addRefreshData(List<S> data);

    public void addLoadMoreSpinner() {
        Handler mainHandler = new Handler(getMainLooper());
        mainHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        loadMore = true;
                        notifyItemInserted(data.size());
                    }
                }
        );
    }

    public void removeLoadMoreSpinner() {
        loadMore = false;
        notifyDataSetChanged();
    }

    public void loadMoreEnd(List<S> data, boolean noMore) {
        this.noMore = noMore;
        addLoadMoreData(data);
    }

    public void getRefreshData(List<S> data, boolean noMore) {
        if (this.noMore) {
            notifyItemRemoved(data.size());
        }

        this.noMore = noMore;
        if (this.noMore) {
            notifyItemInserted(data.size());
        }

        addRefreshData(data);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == data.size()) {
            if (loadMore) {
                return LOAD_MORE_SPIN;
            } else if (noMore) {
                return NO_MORE;
            } else {
                throw new Error("PPLoadDataAdapter.getItemViewType中数组越界");
            }
        } else {
            return ppGetItemViewType(position);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == LOAD_MORE_SPIN) {
            ProgressBar progressBar = new ProgressBar(parent.getContext(), null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            progressBar.setLayoutParams(params);

            return new PPLoadMoreViewHolder(progressBar);
        } else if (viewType == NO_MORE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.activity_list_item, parent, false);
            return new PPNoMoreViewHolder(v);
        } else {
            return ppOnCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < data.size()) {
            ppOnBindViewHolder(holder, position);
        } else {
            //do nothing因为PPLoadMoreViewHolder, PPNoMoreViewHolder不需要传入数据
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;

        if (data != null) {
            count = data.size();
        }

        if (loadMore || noMore) {
            count += 1;
        }

        return count;
    }

    public class PPLoadMoreViewHolder extends RecyclerView.ViewHolder {

        public PPLoadMoreViewHolder(View itemView) {

            super(itemView);
        }
    }

    public class PPNoMoreViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public PPNoMoreViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT)));
            textView.setGravity(Gravity.CENTER);
            textView.setText(PPApplication.getContext().getString(R.string.no_more));
        }
    }
}
