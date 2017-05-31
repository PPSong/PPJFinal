package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.app.TakePhotoFragmentActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TResult;
import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.databinding.ActivityMomentCreatingBinding;
import com.penn.ppj.messageEvent.MomentCreatedEvent;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.PPPagerAdapter;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.realm.ObjectChangeSet;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmObjectChangeListener;

import static com.penn.ppj.PPApplication.getLatestGeoString;

public class MomentCreatingActivity extends TakePhotoActivity {
    //变量
    private ActivityMomentCreatingBinding binding;
    private Realm realm;
    private MomentCreating momentCreating;
    private RealmObjectChangeListener<MomentCreating> changeListener;

    //构造函数

    //本类覆盖函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_moment_creating);

        setup();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    //接口覆盖函数
    @Override
    public void takeFail(TResult result, String msg) {
        PPApplication.error(msg);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            MomentCreating momentCreating = realm.where(MomentCreating.class).equalTo("status", MomentStatus.PREPARE.toString()).findFirst();

            momentCreating.setPic(result.getImages().get(0).getCompressPath());

            realm.commitTransaction();
        }
    }

    //帮助函数
    private void setup() {
        //设置toolbar高度和padding
        AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) binding.toolbar.getLayoutParams();
        layoutParams.height = PPApplication.getStatusBarAddActionBarHeight();
        binding.toolbar.setLayoutParams(layoutParams);
        binding.toolbar.setPadding(0, PPApplication.getStatusBarHeight(), 0, 0);

        realm = Realm.getDefaultInstance();
        setupBindingData();

        //go back button
        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //take photo button
        binding.takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(true);
            }
        });

        //choose image button
        binding.chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(false);
            }
        });

        //发布按钮监控
        Observable<Object> publishButtonObservable = RxView.clicks(binding.publishButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        publishButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                publishMoment();
                            }
                        }
                );

        Observable<CharSequence> contentObservable = RxTextView.textChanges(binding.content)
                .debounce(200, TimeUnit.MILLISECONDS);

        contentObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<CharSequence>() {
                            @Override
                            public void accept(@NonNull CharSequence charSequence) throws Exception {
                                realm.beginTransaction();
                                momentCreating.setContent("" + charSequence);
                                realm.commitTransaction();
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPApplication.error(throwable.toString());
                            }
                        }
                );

        getCurLocation();
    }

    private void setupBindingData() {
        momentCreating = realm.where(MomentCreating.class).equalTo("status", MomentStatus.PREPARE.toString()).findFirst();
        if (momentCreating == null) {
            momentCreating = new MomentCreating();
            momentCreating.setKey();
            momentCreating.setStatus(MomentStatus.PREPARE);
            realm.beginTransaction();

            momentCreating = realm.copyToRealm(momentCreating);

            realm.commitTransaction();
        }


        momentCreating.addChangeListener(new RealmObjectChangeListener<MomentCreating>() {
            @Override
            public void onChange(MomentCreating object, ObjectChangeSet changeSet) {
                if (object.isValid()) {
                    binding.setData(object);
                    validatePublish();
                }
            }
        });

        binding.setData(momentCreating);
        binding.content.setText(momentCreating.getContent());
    }

    private void getCurLocation() {
        realm.beginTransaction();
        momentCreating.setGeo(PPApplication.getLatestGeoString());
        momentCreating.setAddress(PPApplication.getLatestAddress());
        realm.commitTransaction();
    }

    private void validatePublish() {
        String result = momentCreating.validatePublish();
        if (!result.equalsIgnoreCase("OK")) {
            binding.publishButton.setEnabled(false);
        } else {
            binding.publishButton.setEnabled(true);
        }
    }

    private void publishMoment() {
        //把momentCreating放入Moment
        Moment moment = new Moment();
        moment.setKey(momentCreating.getKey());
        moment.setUserId(PPApplication.getCurrentUserId());
        moment.setCreateTime(momentCreating.getCreateTime());
        moment.setStatus(MomentStatus.LOCAL);
        Pic pic = new Pic();
        pic.setKey(momentCreating.getKey() + "_" + 0);
        pic.setStatus(PicStatus.LOCAL);
        pic.setLocalData(momentCreating.getPic());
        moment.setPic(pic);

        realm.beginTransaction();

        momentCreating.setStatus(MomentStatus.LOCAL);
        realm.insert(moment);

        realm.commitTransaction();

        binding.content.setText("");

        //upload this moment
        PPApplication.uploadMoment(momentCreating.getKey());

        //通知新建moment成功
        EventBus.getDefault().post(new MomentCreatedEvent(momentCreating.getKey()));
        finish();
    }

    private void takePhoto(boolean camera) {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + "tmp.jpg");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        Uri imageUri = Uri.fromFile(file);

        CompressConfig config = new CompressConfig.Builder()
                .setMaxSize(1024 * 1024)
                .setMaxPixel(1024)
                .create();

        TakePhoto takePhoto = getTakePhoto();

        takePhoto.onEnableCompress(config, true);

        if (camera) {
            takePhoto.onPickFromCapture(imageUri);
        } else {
            takePhoto.onPickFromGallery();
        }
    }
}
