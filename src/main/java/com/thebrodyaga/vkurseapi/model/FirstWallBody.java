package com.thebrodyaga.vkurseapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FirstWallBody implements Serializable {
    public Integer timeStep;
    public List<OwnerInfo> ownerInfoList = new ArrayList<>();
}
