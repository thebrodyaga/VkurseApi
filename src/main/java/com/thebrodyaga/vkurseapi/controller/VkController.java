package com.thebrodyaga.vkurseapi.controller;

import com.thebrodyaga.vkurseapi.Application;
import com.thebrodyaga.vkurseapi.model.GetVkWallBody;
import com.thebrodyaga.vkurseapi.model.VkWall;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.groups.responses.GetMembersResponse;
import com.vk.api.sdk.objects.groups.responses.SearchResponse;
import com.vk.api.sdk.queries.groups.GroupField;
import com.vk.api.sdk.queries.groups.GroupsGetByIdQuery;
import com.vk.api.sdk.queries.groups.GroupsGetMembersQuery;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vk")
public class VkController {
    public static int timeStep = 60 * 60 * 2; //2 часа,
    public static int postCount = 20; //кол-во постов,

    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code) {
        String codeEE = code;
    }

    @PostMapping("/firstWall")
    public VkWall firstWall(@RequestBody GetVkWallBody firstWallBody) {
        return new VkWall().getFirstWall(firstWallBody);
    }

    @PostMapping("/wallAfterLast")
    public VkWall wallAfterLast(@RequestBody GetVkWallBody afterLastBody) {
        return new VkWall().getWallAfterLast(afterLastBody);
    }

    @PostMapping("/newWall")
    public VkWall getNewWall(@RequestBody GetVkWallBody newWallBody) {
        return new VkWall().getNewWall(newWallBody);
    }

    @GetMapping("/searchGroups")
    public SearchResponse searchGroups(@RequestParam() String q,
                                       @RequestParam(defaultValue = "0") Integer offset) throws ClientException, ApiException {
        return Application.getVk().groups().search(Application.getUserActor(), q)
                .offset(offset).execute();
    }

    @GetMapping("/getGroupsById")
    public List<GroupFull> getGroupsById(@RequestParam(required = false) String groupId,
                                         @RequestParam(required = false) String... groupIds) throws ClientException, ApiException {
        GroupsGetByIdQuery query = Application.getVk().groups().getById(Application.getServiceActor());
        if (groupId != null)
            query.groupId(groupId);
        if (groupIds != null)
            query.groupIds(groupIds);
        return query.execute();
    }

    @GetMapping("/getSingleGroup")
    public List<GroupFull> getSingleGroup(@RequestParam() String groupId) throws ClientException, ApiException {
        GroupsGetByIdQuery query = Application.getVk().groups().getById(Application.getServiceActor());
        query.groupId(groupId)
                .fields(GroupField.CITY, GroupField.CONTACTS, GroupField.COVER, GroupField.DESCRIPTION,
                        GroupField.FIXED_POST, GroupField.LINKS, GroupField.MAIN_ALBUM_ID,GroupField.PLACE,
                        GroupField.SITE,GroupField.STATUS,GroupField.VERIFIED,
                        GroupField.COUNTERS, GroupField.LINKS);
        return query.execute();
    }

    @GetMapping("/getGroupsMembers")
    public GetMembersResponse getGroupsMembers(@RequestParam() String groupId) throws ClientException, ApiException {
        return Application.getVk().groups()
                .getMembers(Application.getServiceActor())
                .groupId(groupId).execute();
    }
}
