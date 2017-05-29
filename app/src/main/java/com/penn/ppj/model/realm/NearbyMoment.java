package com.penn.ppj.model.realm;

import com.penn.ppj.ppEnum.MomentStatus;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/05/2017.
 */

public class NearbyMoment extends RealmObject {
    @PrimaryKey
    private String key;//createTime_creatorUserId
    private String id;
    private long createTime;
    private String pic;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }
}
