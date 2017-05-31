package com.penn.ppj;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.google.gson.JsonArray;
import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.databinding.ActivityMomentDetailBinding;
import com.penn.ppj.databinding.CommentCellBinding;
import com.penn.ppj.databinding.MomentDetailHeadBinding;
import com.penn.ppj.messageEvent.MomentDeleteEvent;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.ppEnum.CommentStatus;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPPagerAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.ObjectChangeSet;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmObjectChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.os.Build.VERSION_CODES.M;
import static com.penn.ppj.PPApplication.error;
import static com.penn.ppj.PPApplication.ppFromString;
import static com.penn.ppj.PPApplication.ppWarning;

public class MomentDetailActivity extends AppCompatActivity {
    //变量
    private ActivityMomentDetailBinding binding;
    private String momentId;
    private Realm realm;
    private MomentDetail momentDetail;
    private RealmResults<Comment> comments;
    private PPAdapter ppAdapter;
    private LinearLayoutManager linearLayoutManager;
    private MomentDetailHeadBinding momentDetailHeadBinding;
    private CommentInputBottomSheetDialogFragment.CommentViewModel commentViewModel;

    private int titleHeight;
    private int headPicHeight;
    private int headPicMinHeight;
    private int floatingButtonHalfHeight;

    private boolean likeButtonSetuped = false;

    private class PPAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int HEAD = 1;
        private static final int COMMENT = 2;

        private List<Comment> data;

        public class PPHoldView extends RecyclerView.ViewHolder {
            private CommentCellBinding binding;

            public PPHoldView(CommentCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public class PPHead extends RecyclerView.ViewHolder {
            private MomentDetailHeadBinding binding;

            public PPHead(MomentDetailHeadBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                binding.setData(momentDetail);
            }
        }

        public PPAdapter(List<Comment> data) {
            this.data = data;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == HEAD) {
                final MomentDetailHeadBinding tmpMomentDetailHeadBinding = MomentDetailHeadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

                momentDetailHeadBinding = tmpMomentDetailHeadBinding;

                momentDetailHeadBinding.avatarCircleImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (momentDetail.getUserId().equals(PPApplication.getCurrentUserId())) {
                            Intent intent = new Intent(MomentDetailActivity.this, MyProfileActivity.class);
                            MomentDetailActivity.this.startActivity(intent);
                        } else {
                            Intent intent = new Intent(MomentDetailActivity.this, UserHomePageActivity.class);
                            intent.putExtra("userId", momentDetail.getUserId());
                            startActivity(intent);
                        }
                    }
                });

                return new PPHead(tmpMomentDetailHeadBinding);
            } else if (viewType == COMMENT) {
                CommentCellBinding commentCellBinding = CommentCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

                commentCellBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = binding.recyclerView.getChildAdapterPosition(v);
                        //由于第一个cell被head占了
                        Comment comment = comments.get(position - 1);

                        if (!comment.getUserId().equals(PPApplication.getCurrentUserId())) {
                            commentTo(comment);
                        }
                    }
                });

                return new PPHoldView(commentCellBinding);
            } else {
                throw new Error("MomentDetailActivity中comment没有找到viewType");
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                ((PPHead) holder).binding.setData(momentDetail);
            } else {
                final Comment comment = data.get(position - 1);
                ((PPHoldView) holder).binding.setData(comment);

                ((PPHoldView) holder).binding.avatarCircleImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String userId = comment.getUserId();
                        if (userId.equals(PPApplication.getCurrentUserId())) {
                            Intent intent = new Intent(MomentDetailActivity.this, MyProfileActivity.class);
                            MomentDetailActivity.this.startActivity(intent);
                        } else {
                            Intent intent = new Intent(MomentDetailActivity.this, UserHomePageActivity.class);
                            intent.putExtra("userId", userId);
                            startActivity(intent);
                        }
                    }
                });

                ((PPHoldView) holder).binding.deleteImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (comment.getStatus().equals(CommentStatus.NET)) {
                            final String commentId = comment.getId();
                            final String momentId = comment.getMomentId();
                            //确认对话框
                            AlertDialog.Builder alert = new AlertDialog.Builder(
                                    MomentDetailActivity.this);
                            alert.setTitle(getString(R.string.warn));
                            alert.setMessage(getString(R.string.delete_are_you_sure));
                            alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //标记本地comment deleted为true
                                    try (Realm realm = Realm.getDefaultInstance()) {
                                        realm.beginTransaction();

                                        realm.where(Comment.class).equalTo("id", commentId).findFirst().setDeleted(true);

                                        realm.commitTransaction();
                                    }
                                    PPApplication.removeComment(commentId, momentId);
                                    dialog.dismiss();
                                }
                            });
                            alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    dialog.dismiss();
                                }
                            });

                            alert.show();
                        }
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? HEAD : COMMENT;
        }

        @Override
        public int getItemCount() {
            return data.size() + 1;
        }
    }

    //构造函数

    //本类覆盖函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_moment_detail);

        setup();
    }

    //接口覆盖函数

    //帮助函数
    private void setup() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PPApplication.getStatusBarAddActionBarHeight());
        binding.toolbarConstraintLayout.setLayoutParams(params);

        realm = Realm.getDefaultInstance();

        Intent intent = getIntent();
        momentId = intent.getStringExtra("momentId");

        binding.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        comments = realm.where(Comment.class).equalTo("momentId", momentId).notEqualTo("deleted", true).findAllSorted("createTime", Sort.DESCENDING);
        comments.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<Comment>>() {
            @Override
            public void onChange(RealmResults<Comment> collection, OrderedCollectionChangeSet changeSet) {
                // `null`  means the async query returns the first time.
                if (changeSet == null) {
                    ppAdapter.notifyDataSetChanged();
                    return;
                }
                // For deletions, the adapter has to be notified in reverse order.
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    ppAdapter.notifyItemRangeRemoved(range.startIndex + 1, range.length);
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    ppAdapter.notifyItemRangeInserted(range.startIndex + 1, range.length);
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    ppAdapter.notifyItemRangeChanged(range.startIndex + 1, range.length);
                }
            }
        });

        ppAdapter = new PPAdapter(comments);
        linearLayoutManager = new LinearLayoutManager(this);

        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(ppAdapter);

        binding.recyclerView.setHasFixedSize(true);

        final int headMinHeadHeight = PPApplication.calculateHeadMinHeight();

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!likeButtonSetuped) {
                    return;
                }

                final int scrollY = momentDetailHeadBinding.getRoot().getTop();

                int fabBan = headPicHeight - headMinHeadHeight + titleHeight;
                int imageBan = headPicHeight - headMinHeadHeight;

                if (Math.abs(scrollY) < fabBan) {
                    binding.commentFloatingActionButton.setTranslationY(scrollY);
                } else {
                    binding.commentFloatingActionButton.setTranslationY(-fabBan);
                }

                if (Math.abs(scrollY) < imageBan) {
                    binding.mainImageContainerFrameLayout.setElevation(0);
                    binding.mainImageContainerFrameLayout.setTranslationY(scrollY);
                } else {
                    binding.mainImageContainerFrameLayout.setElevation(11);
                    binding.mainImageContainerFrameLayout.setTranslationY(-imageBan);
                }
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

        //deleteMoment按钮监控
        RxView.clicks(binding.deleteMomentImageButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                //确认对话框
                                AlertDialog.Builder alert = new AlertDialog.Builder(
                                        MomentDetailActivity.this);
                                alert.setTitle(getString(R.string.warn));
                                alert.setMessage(getString(R.string.delete_are_you_sure));
                                alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //do your work here
                                        //标记本地moment deleted为true, 删除本地momentDetail和相关comments
                                        try (Realm realm = Realm.getDefaultInstance()) {
                                            realm.beginTransaction();

                                            realm.where(MomentDetail.class).equalTo("id", momentId).findFirst().deleteFromRealm();
                                            realm.where(Comment.class).equalTo("momentId", momentId).findAll().deleteAllFromRealm();
                                            realm.where(Moment.class).equalTo("id", momentId).findFirst().setDeleted(true);

                                            realm.commitTransaction();
                                        }
                                        finish();
                                        EventBus.getDefault().post(new MomentDeleteEvent(momentId));
                                        dialog.dismiss();
                                    }
                                });
                                alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        dialog.dismiss();
                                    }
                                });

                                alert.show();
                            }
                        }
                );

        //like按钮监控
        Observable<Object> likeButtonObservable = RxView.clicks(binding.likeToggleButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        likeButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                likeOrUnlikeMoment(binding.likeToggleButton.isChecked());
                            }
                        }
                );

        //init commentViewModel
        commentViewModel = new CommentInputBottomSheetDialogFragment.CommentViewModel();

        //showComment按钮监控
        RxView.clicks(binding.commentFloatingActionButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                if (commentViewModel.targetUserId != "") {
                                    resetComment();
                                    setTarget(null);
                                }
                                showCommentInput();
                            }
                        }
                );

        //由于momentDetailHeadBinding是在onCreateViewHolder中初始化的, 怀疑 ppAdapter = new PPAdapter(comments);是个异步操作
        //这里不用延时的话会导致momentDetailHeadBinding null错误
        binding.recyclerView.post(new

                                          Runnable() {
                                              @Override
                                              public void run() {
                                                  momentDetail = realm.where(MomentDetail.class).equalTo("id", momentId).findFirst();
                                                  if (momentDetail != null) {
                                                      firstSetBinding(momentDetail);
                                                  }

                                                  getServerMomentDetail();
                                              }
                                          });

//        binding.mainImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ImageViewerActivity.start(MomentDetailActivity.this, PPHelper.get800ImageUrl(momentDetail.getPic()), binding.mainImageView);
//            }
//        });
    }

    private void likeOrUnlikeMoment(boolean like) {
        String api = like ? "moment.like" : "moment.unLike";

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("id", momentId);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api(api, jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);

                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        //请求服务器最新MomentDetail记录
                        PPJSONObject jBody1 = new PPJSONObject();
                        jBody1
                                .put("id", momentId)
                                .put("checkFollow", "1")
                                .put("checkLike", "1");

                        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("moment.detail", jBody1.getJSONObject());

                        return apiResult1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                Log.v("pplog560", "likeOrUnlikeMoment:" + s);
                                updateLocalMomentDetail(s);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPApplication.error(throwable.toString());
                                restoreLocalMomentDetail();
                            }
                        }
                );
    }

    private void updateLocalMomentDetail(String momentDetailString) {
        String momentId = ppFromString(momentDetailString, "data._id").getAsString();

        realm.beginTransaction();

        long now = System.currentTimeMillis();

        MomentDetail momentDetail = realm.where(MomentDetail.class).equalTo("id", momentId).findFirst();
        momentDetail.setLiked(ppFromString(momentDetailString, "data.isLiked").getAsInt() == 1 ? true : false);
        momentDetail.setLastVisitTime(now);

        realm.commitTransaction();

        binding.setData(momentDetail);
    }

    private void restoreLocalMomentDetail() {
        binding.setData(momentDetail);
    }

    private void getServerMomentDetail() {
        //请求服务器最新MomentDetail记录
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("id", momentId)
                .put("checkFollow", "1")
                .put("checkLike", "1");

        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("moment.detail", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("id", momentId)
                .put("beforeTime", "")
                .put("afterTime", "1");

        final Observable<String> apiResult2 = PPRetrofit.getInstance().api("moment.getReplies", jBody2.getJSONObject());

        Observable
                .zip(
                        apiResult1, apiResult2, new BiFunction<String, String, String[]>() {

                            @Override
                            public String[] apply(@NonNull String s, @NonNull String s2) throws Exception {
                                String[] result = {s, s2};

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
                                //更新本地最新MomentDetail记录到最新MomentDetail记录
                                PPWarn ppWarn = ppWarning(result[0]);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPWarn ppWarn2 = ppWarning(result[1]);

                                if (ppWarn2 != null) {
                                    throw new Exception(ppWarn2.msg);
                                }

                                processMomentDetailAndComments(result[0], result[1]);

                                //动态显示comment fab
                                setupCommentButton();

                                if (momentDetail == null) {
                                    momentDetail = realm.where(MomentDetail.class).equalTo("id", momentId).findFirst();
                                    firstSetBinding(momentDetail);
                                }
                            }
                        }
                        ,
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                            }
                        }
                );
    }

    private void firstSetBinding(MomentDetail momentDetail) {
        //如果本地有记录缓存, 取消loading modal
        binding.progressFrameLayout.setVisibility(View.INVISIBLE);
        //修改本地记录lastVisitTime
        realm.beginTransaction();
        momentDetail.setLastVisitTime(System.currentTimeMillis());
        realm.commitTransaction();

        momentDetail.addChangeListener(new RealmObjectChangeListener<MomentDetail>() {
            @Override
            public void onChange(MomentDetail object, ObjectChangeSet changeSet) {
                if (object.isValid()) {
                    binding.setData(object);
                    momentDetailHeadBinding.setData(object);
                }
            }
        });

        //set binding data
        binding.setData(momentDetail);
        momentDetailHeadBinding.setData(momentDetail);
    }

    private void processMomentDetailAndComments(String momentDetailString, String commentsString) {
        //构造comment detail和comments
        long now = System.currentTimeMillis();

        MomentDetail momentDetail = new MomentDetail();
        momentDetail.setId(ppFromString(momentDetailString, "data._id").getAsString());
        momentDetail.setGeo(ppFromString(momentDetailString, "data.location.geo.0").getAsString() + "," + ppFromString(momentDetailString, "data.location.geo.1").getAsString());
        momentDetail.setAddress(ppFromString(momentDetailString, "data.location.detail").getAsString());
        momentDetail.setCity(ppFromString(momentDetailString, "data.location.city").getAsString());
        momentDetail.setUserId(ppFromString(momentDetailString, "data._creator.id").getAsString());
        momentDetail.setContent(ppFromString(momentDetailString, "data.content").getAsString());
        momentDetail.setLiked(ppFromString(momentDetailString, "data.isLiked").getAsInt() == 1 ? true : false);
        momentDetail.setCreateTime(ppFromString(momentDetailString, "data.createTime").getAsLong());
        momentDetail.setPic(ppFromString(momentDetailString, "data.pics.0").getAsString());
        momentDetail.setAvatar(ppFromString(momentDetailString, "data._creator.head").getAsString());
        momentDetail.setNickname(ppFromString(momentDetailString, "data._creator.nickname").getAsString());
        momentDetail.setLastVisitTime(now);

        JsonArray ja = ppFromString(commentsString, "data.list").getAsJsonArray();

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            try {
                for (int i = 0; i < ja.size(); i++) {

                    long createTime = ppFromString(commentsString, "data.list." + i + ".createTime").getAsLong();
                    String userId = ppFromString(commentsString, "data.list." + i + "._creator.id").getAsString();

                    Comment comment = realm.where(Comment.class).equalTo("key", createTime + "_" + userId).equalTo("deleted", true).findFirst();
                    if (comment != null) {
                        //如果这条记录本地已经标志为deleted, 则跳过
                        continue;
                    }

                    comment = new Comment();

                    comment.setKey(createTime + "_" + userId);
                    comment.setId(ppFromString(commentsString, "data.list." + i + "._id").getAsString());
                    comment.setUserId(ppFromString(commentsString, "data.list." + i + "._creator.id").getAsString());
                    comment.setMomentId(momentId);
                    comment.setContent(ppFromString(commentsString, "data.list." + i + ".content").getAsString());
                    comment.setCreateTime(createTime);
                    comment.setNickname(ppFromString(commentsString, "data.list." + i + "._creator.nickname").getAsString());
                    comment.setAvatar(ppFromString(commentsString, "data.list." + i + "._creator.head").getAsString());
                    comment.setStatus(CommentStatus.NET);
                    comment.setReferUserId(ppFromString(commentsString, "data.list." + i + ".refer.id", PPValueType.STRING).getAsString());
                    comment.setReferNickname(ppFromString(commentsString, "data.list." + i + ".refer.nickname", PPValueType.STRING).getAsString());
                    comment.setBePrivate(ppFromString(commentsString, "data.list." + i + ".isPrivate").getAsBoolean());
                    comment.setLastVisitTime(now);
                    realm.insertOrUpdate(comment);
                }

                //把服务器上已删除的comment从本地删掉
                realm.where(Comment.class).equalTo("momentId", momentId).notEqualTo("lastVisitTime", now).findAll().deleteAllFromRealm();

                realm.insertOrUpdate(momentDetail);

                realm.commitTransaction();
            } catch (Exception e) {
                error(e.toString());
                realm.cancelTransaction();
            }

        }
    }

    private void setupCommentButton() {
        titleHeight = momentDetailHeadBinding.contentTextView.getHeight();
        headPicHeight = PPApplication.calculateHeadHeight();
        headPicMinHeight = PPApplication.calculateHeadMinHeight();
        floatingButtonHalfHeight = binding.commentFloatingActionButton.getHeight() / 2;

        likeButtonSetuped = true;

        commentButtonAppear(binding.commentFloatingActionButton, titleHeight + headPicHeight - floatingButtonHalfHeight);
    }

    public void commentButtonAppear(View floatingActionButton, int topMargin) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) floatingActionButton.getLayoutParams();
        params.topMargin = topMargin;
        floatingActionButton.setLayoutParams(params);

        floatingActionButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.appear));
    }

    private void setTarget(Comment comment) {
        if (comment != null) {
            commentViewModel.targetUserId = comment.getUserId();
            commentViewModel.targetNickname = comment.getNickname();
        } else {
            commentViewModel.targetUserId = "";
            commentViewModel.targetNickname = "";
        }
    }

    private void resetComment() {
        commentViewModel.reset();
    }

    private void showCommentInput() {
        CommentInputBottomSheetDialogFragment commentInputBottomSheetDialogFragment = CommentInputBottomSheetDialogFragment.newInstance(commentViewModel);
        commentInputBottomSheetDialogFragment.setCommentInputBottomSheetDialogFragmentListener(new CommentInputBottomSheetDialogFragment.CommentInputBottomSheetDialogFragmentListener() {
            @Override
            public void setCommentViewModel(CommentInputBottomSheetDialogFragment.CommentViewModel commentViewModel) {
                MomentDetailActivity.this.commentViewModel = commentViewModel;
            }

            @Override
            public void sendComment() {
                //插入到本地数据库
                final long now = System.currentTimeMillis();

                CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();

                final Comment comment = new Comment();
                comment.setKey(now + "_" + currentUser.getUserId());
                comment.setUserId(currentUser.getUserId());
                comment.setMomentId(momentId);
                comment.setCreateTime(now);
                comment.setNickname(currentUser.getNickname());
                comment.setAvatar(currentUser.getAvatar());
                comment.setContent(commentViewModel.content);

                comment.setBePrivate(commentViewModel.bePrivate);

                comment.setStatus(CommentStatus.LOCAL);
                comment.setLastVisitTime(now);

                realm.beginTransaction();

                realm.insert(comment);

                realm.commitTransaction();

                binding.recyclerView.smoothScrollToPosition(0);

                //发送comment
                PPJSONObject jBody = new PPJSONObject();
                jBody
                        .put("id", momentId)
                        .put("content", commentViewModel.content)
                        .put("refer", commentViewModel.targetUserId)
                        .put("isPrivate", "" + commentViewModel.bePrivate);

                final Observable<String> apiResult = PPRetrofit.getInstance()
                        .api("moment.reply", jBody.getJSONObject());

                final Comment comment2 = realm.where(Comment.class).equalTo("key", now + "_" + PPApplication.getCurrentUserId()).findFirst();

                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(@NonNull String s) throws Exception {

                                        PPWarn ppWarn = PPApplication.ppWarning(s);

                                        if (ppWarn != null) {
                                            throw new Exception(ppWarn.msg);
                                        }

                                        getServerMomentDetail();
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(@NonNull Throwable throwable) throws Exception {
                                        realm.beginTransaction();
                                        comment2.setStatus(CommentStatus.FAILED);
                                        realm.copyToRealm(comment2);

                                        realm.commitTransaction();
                                        PPApplication.error(throwable.toString());
                                    }
                                }
                        );

                resetComment();
            }
        });
        commentInputBottomSheetDialogFragment.show(getSupportFragmentManager(), "Dialog");
    }

    private void commentTo(Comment comment) {
        if (!commentViewModel.targetUserId.equals(comment.getUserId())) {
            resetComment();
            setTarget(comment);
        }
        showCommentInput();
    }
}
