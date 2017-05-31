package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jakewharton.rxbinding2.view.RxView;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoFragmentActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TResult;
import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.databinding.ActivityMyProfileBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPPagerAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
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

import static com.penn.ppj.PPApplication.ppFromString;
import static com.penn.ppj.PPApplication.ppWarning;

public class MyProfileActivity extends TakePhotoFragmentActivity {
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
    private void takePhoto() {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + "tmp.jpg");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        Uri imageUri = Uri.fromFile(file);

        CompressConfig config = new CompressConfig.Builder()
                .setMaxSize(1024 * 1024)
                .setMaxPixel(1024)
                .create();

        TakePhoto takePhoto = getTakePhoto();

        takePhoto.onEnableCompress(config, true);

        //pptodo 支持拍摄和选取图片
        //takePhoto.onPickFromCapture(imageUri);
        takePhoto.onPickFromGallery();
    }

    @Override
    public void takeFail(TResult result, String msg) {
        PPApplication.error(msg);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);

        //修改页面上的avatar
        Bitmap myBitmap = BitmapFactory.decodeFile(result.getImages().get(0).getCompressPath());

        binding.avatarImageView.setImageBitmap(myBitmap);

        //上传avatar
        //申请上传图片的token
        final String key = "_" + PPApplication.getCurrentUserId() + "_head_" + System.currentTimeMillis();
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("type", "public")
                .put("filename", key);

        final Observable<String> requestToken = PPRetrofit.getInstance().api("system.generateUploadToken", jBody.getJSONObject());

        //上传avatar
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("head", key);

        final Observable<String> apiResult1 = PPRetrofit.getInstance()
                .api("user.changeHead", jBody1.getJSONObject());


        //File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + "tmp.jpg");
        File file = new File(result.getImages().get(0).getCompressPath());
        int size = (int) file.length();
        final byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.v("pplog", "MomentCreating.setPic error:" + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.v("pplog", "MomentCreating.setPic error:" + e.toString());
            e.printStackTrace();
        }

        requestToken
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(
                        new Function<String, ObservableSource<String>>() {
                            @Override
                            public ObservableSource<String> apply(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg + ":" + key);
                                }
                                String token = ppFromString(s, "data.token").getAsString();
                                return PPApplication.uploadSingleImage(bytes, key, token);
                            }
                        }
                )
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull String s) throws Exception {
                        return apiResult1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg);
                                }

                                realm.beginTransaction();

                                currentUser.setAvatar(key);

                                realm.commitTransaction();

                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    binding.setData(currentUser);
                                }
                                PPApplication.error(throwable.toString());
                            }
                        }
                );

    }

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

        binding.avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    private void showRelatedUsers(RelatedUserType relatedUserType) {
        RelatedUserBottomSheetDialogFragment relatedUserBottomSheetDialogFragment = RelatedUserBottomSheetDialogFragment.newInstance(relatedUserType);
        relatedUserBottomSheetDialogFragment.show(getSupportFragmentManager(), relatedUserBottomSheetDialogFragment.getTag());
    }
}
