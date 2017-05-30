package com.penn.ppj.model.realm;

import com.penn.ppj.ppEnum.RelatedUserType;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/05/2017.
 */

public class RelatedUser extends RealmObject {
    @PrimaryKey
    private String key;
    private String userId;
    private String type;
    private long createTime;
    private String nickname;
    private String avatar;
    private long lastVisitTime;

    public String getKey() {
        return key;
    }

    public void setKey() {
        this.key = userId + "_" + type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public RelatedUserType getType() {
        return RelatedUserType.valueOf(type);
    }

    public void setType(RelatedUserType type) {
        this.type = type.toString();
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public long getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }
}
