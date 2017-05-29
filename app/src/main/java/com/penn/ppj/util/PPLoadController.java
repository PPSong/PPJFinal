package com.penn.ppj.util;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Created by penn on 12/05/2017.
 */

public class PPLoadController extends RecyclerView.OnScrollListener implements SwipeRefreshLayout.OnRefreshListener {
    private boolean dataLoading = false;
    //default value 5
    private int loadMoreDistance = 0;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    public PPLoadDataAdapter ppLoadDataAdapter;
    private LinearLayoutManager linearLayoutManager;
    private LoadDataProvider loadDataProvider;

    public PPLoadController(SwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView, PPLoadDataAdapter ppLoadDataAdapter, LinearLayoutManager linearLayoutManager, LoadDataProvider loadDataProvider) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.recyclerView = recyclerView;
        this.ppLoadDataAdapter = ppLoadDataAdapter;
        this.linearLayoutManager = linearLayoutManager;
        this.loadDataProvider = loadDataProvider;

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(ppLoadDataAdapter);
        recyclerView.addOnScrollListener(this);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    public interface LoadDataProvider {
        //结束需要用ppLoadController.ppLoadDataAdapter.getRefreshData发送得到的数据并用endRefreshSpinner结束refresh spinner
        public void refreshData();

        //结束需要用ppLoadController.ppLoadDataAdapter.loadMoreEnd发送得到的数据并用removeLoadMoreSpinner结束load more spinner
        public void loadMoreData();
    }

    public void endRefreshSpinner() {
        dataLoading = false;
        swipeRefreshLayout.setRefreshing(false);
    }

    public void removeLoadMoreSpinner() {
        dataLoading = false;
        ppLoadDataAdapter.removeLoadMoreSpinner();
    }

    @Override
    public void onRefresh() {
        if (dataLoading) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        dataLoading = true;
        loadDataProvider.refreshData();
    }

    public void loadMore() {
        dataLoading = true;
        ppLoadDataAdapter.addLoadMoreSpinner();
        loadDataProvider.loadMoreData();
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (dy < 0 || dataLoading || ppLoadDataAdapter.noMore) {
            return;
        }

        final int visibleItemCount = recyclerView.getChildCount();
        final int totalItemCount = linearLayoutManager.getItemCount();
        final int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();

        if (!(ppLoadDataAdapter.noMore) && (totalItemCount - visibleItemCount) <= (firstVisibleItem + loadMoreDistance)) {
            loadMore();
        }
    }
}
