package com.thebrodyaga.vkurseapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VkWallsFromClient implements Serializable {
    public Integer lastPostDate;
    public List<VkPostSourceInfo> wallsFromClient = new ArrayList<>();
}
