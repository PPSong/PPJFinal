package com.penn.ppj.model.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/05/2017.
 */

public class UserHomePage extends RealmObject {
    @PrimaryKey
    private String userId;
    private String nickname;
    private String avatar;
    private int meets;
    private int collects;
    private int beCollecteds;
    private boolean isFollowed;
    private long lastVisitTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public boolean isFollowed() {
        return isFollowed;
    }

    public void setFollowed(boolean followed) {
        isFollowed = followed;
    }

    public long getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }

    public int getMeets() {
        return meets;
    }

    public void setMeets(int meets) {
        this.meets = meets;
    }

    public int getCollects() {
        return collects;
    }

    public void setCollects(int collects) {
        this.collects = collects;
    }

    public int getBeCollecteds() {
        return beCollecteds;
    }

    public void setBeCollecteds(int beCollecteds) {
        this.beCollecteds = beCollecteds;
    }
}
