package com.thebrodyaga.vkurseapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GetVkWallBody implements Serializable {
    /**
     * Время последнего поста на клиенте. null при пустой стене на клиенте
     */
    public Integer lastPostDate;
    /**
     * Время первого поста на клиенте. not null только при обновлении стены
     */
    public Integer firstPostDate;
    /**
     * Временной промежуток необходимых постов
     */
    public Integer timeStep;
    /**
     * Список стен(источников постов) offset == null or offset == 0 при пустой стене на клиенте
     */
    public List<OwnerInfo> ownerInfoList = new ArrayList<>();
}
