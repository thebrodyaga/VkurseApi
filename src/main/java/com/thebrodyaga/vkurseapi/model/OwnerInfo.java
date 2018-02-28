package com.thebrodyaga.vkurseapi.model;

public class OwnerInfo {
    public Integer ownerId;
    public Integer offset;
    public Integer count;

    public OwnerInfo() {
    }

    public OwnerInfo(Integer ownerId, int offset, Integer count) {
        this.ownerId = ownerId;
        this.offset = offset;
        this.count = count;
    }
}
