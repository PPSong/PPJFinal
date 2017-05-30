package com.penn.ppj.model.realm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.penn.ppj.PPApplication;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.PPHelper;

import java.io.ByteArrayOutputStream;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 09/04/2017.
 */

public class Pic extends RealmObject {
    @PrimaryKey
    private String key; //netFileName or createTime_userId_index
    private String status;
    private byte[] localData;
    private byte[] thumbLocalData;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public PicStatus getStatus() {
        return PicStatus.valueOf(status);
    }

    public void setStatus(PicStatus status) {
        this.status = status.toString();
    }

    public byte[] getLocalData() {
        return localData;
    }

    public void setLocalData(byte[] localData) {

        long now = System.currentTimeMillis();

        this.localData = localData;

        Bitmap bmp = BitmapFactory.decodeByteArray(localData, 0, localData.length);

        Bitmap thumbBmp = PPApplication.resize(bmp, 180, 180);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        thumbBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        byte[] byteArray = stream.toByteArray();

        setThumbLocalData(byteArray);
    }

    public byte[] getThumbLocalData() {
        return thumbLocalData;
    }

    public void setThumbLocalData(byte[] thumbLocalData) {
        this.thumbLocalData = thumbLocalData;
    }
}
