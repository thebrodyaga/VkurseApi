package com.thebrodyaga.vkurseapi.model;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.thebrodyaga.vkurseapi.Application;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.users.UserFull;
import com.vk.api.sdk.objects.wall.WallPostFull;
import com.vk.api.sdk.objects.wall.responses.GetExtendedResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.thebrodyaga.vkurseapi.controller.VkController.postCount;
import static com.thebrodyaga.vkurseapi.controller.VkController.timeStep;

public class VkWall {
    private List<OwnerInfo> ownerInfoList = new ArrayList<>();
    private List<WallPostFull> wallPostList = new ArrayList<>();
    private Set<UserFull> profiles = new HashSet<>();
    private Set<GroupFull> groups = new HashSet<>();

    public VkWall getFirstWall(GetVkWallBody firstWallBody) {
        return getWall(firstWallBody, FIRST_WALL_FLAG);
    }

    public VkWall getWallAfterLast(GetVkWallBody afterLastBody) {
        return getWall(afterLastBody, AFTER_LAST_WALL_FLAG);
    }

    public VkWall getNewWall(GetVkWallBody refreshBody) {
        return getWall(refreshBody, NEW_WALL_FLAG);
    }

    public static final int NEW_WALL_FLAG = 0;
    public static final int FIRST_WALL_FLAG = 1;
    public static final int AFTER_LAST_WALL_FLAG = 2;

    /**
     * vkWallBody.lastPostDate     обязательный параметр при isFirstWall == false
     * vkWallBody.timeStep         при null vkWallBody.timeStep = VkController.timeStep
     * if (isFirstWall==true)
     * получаю посты от каждого ownerId в промежутке от первого незакрепленного date до (date - timeStep)
     * сортирую по дате, удаляю от первого date до (date - timeStep), проставляю offset в оставшихся
     * else
     * получаю готовые посты в промежутке, сортирую результат
     */
    private VkWall getWall(GetVkWallBody vkWallBody, @NotNull int neededWall) {
        if (vkWallBody.timeStep == null)
            vkWallBody.timeStep = timeStep;
        ExecutorService service = Executors.newCachedThreadPool();
        for (OwnerInfo source : vkWallBody.ownerInfoList) {
            if (source.ownerId == null || source.ownerId == 0)
                continue;
            service.submit(() -> {
                try {
                    if (source.offset == null)
                        source.offset = 0;
                    VkWallResponse currentResponse;
                    switch (neededWall) {
                        case FIRST_WALL_FLAG:
                            currentResponse = getFirstPosts(source, vkWallBody.timeStep, source.offset);
                            break;
                        case AFTER_LAST_WALL_FLAG:
                            currentResponse = getAfterLastPosts(source, vkWallBody.lastPostDate - vkWallBody.timeStep, source.offset);
                            break;
                        case NEW_WALL_FLAG:
                            currentResponse = getNewWall(source, vkWallBody.firstPostDate);
                            break;
                        default:
                            throw new RuntimeException("Хуйня пришла");
                    }
                    synchronized (VkWall.this) {
                        Integer offset;
                        switch (neededWall) {
                            case FIRST_WALL_FLAG:
                                offset = 0;
                                break;
                            case AFTER_LAST_WALL_FLAG:
                                offset = currentResponse.resultList.size()
                                        + (source.offset != null ? source.offset : 0);
                                break;
                            case NEW_WALL_FLAG:
                                offset = source.offset;
                                break;
                            default:
                                throw new RuntimeException("Хуйня пришла");
                        }
                        wallPostList.addAll(currentResponse.resultList);
                        profiles.addAll(currentResponse.profiles);
                        groups.addAll(currentResponse.groups);
                        ownerInfoList.add(new OwnerInfo(source.ownerId, offset, currentResponse.count));
                    }
                } catch (ClientException | ApiException e) {
                    e.printStackTrace();
                }
            });
        }
        try {
            service.shutdown();
            service.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*
          При добавлении нового поста в промежутке между запросами, удаляются посты раньше времени клиента,
          offset не изменяется, чтоб в следующем запросе учитывать новый пост
         */
        if (neededWall == AFTER_LAST_WALL_FLAG)
            this.wallPostList.removeIf(wallPostFull -> wallPostFull.getDate() >= vkWallBody.lastPostDate);
        if (neededWall == NEW_WALL_FLAG)
            this.wallPostList.removeIf(wallPostFull -> wallPostFull.getDate() <= vkWallBody.firstPostDate);
        this.wallPostList.sort((o1, o2) -> o2.getDate().compareTo(
                o1.getDate()));
        System.out.println();
        if (neededWall == AFTER_LAST_WALL_FLAG)
            return this;
        if (neededWall == FIRST_WALL_FLAG) {
            Integer lastPostDate = wallPostList.get(0).getDate() - vkWallBody.timeStep;
            wallPostList.removeIf(wallPostFull -> wallPostFull.getDate() < lastPostDate);
        }
        for (OwnerInfo sourceResult : ownerInfoList)
            sourceResult.offset = (neededWall == NEW_WALL_FLAG ? sourceResult.offset : 0) + Math.toIntExact(wallPostList
                    .stream()
                    .filter(wallPostFull
                            -> wallPostFull.getOwnerId().equals(sourceResult.ownerId))
                    .count());
        return this;
    }

    private VkWallResponse getAfterLastPosts(@NotNull OwnerInfo ownerInfo, @NotNull Integer lastPostDate,
                                             @NotNull Integer offset) throws ClientException, ApiException {
        return getPostsFromWall(ownerInfo, lastPostDate, null, offset, false);
    }

    private VkWallResponse getFirstPosts(@NotNull OwnerInfo ownerInfo, @NotNull Integer timeStep,
                                         @NotNull Integer offset) throws ClientException, ApiException {
        return getPostsFromWall(ownerInfo, null, timeStep, offset, true);
    }

    private VkWallResponse getNewWall(@NotNull OwnerInfo ownerInfo,
                                      @NotNull Integer lastPostDate) throws ClientException, ApiException {
        return getPostsFromWall(ownerInfo, lastPostDate, null, 0, false);
    }

    /**
     * @param ownerInfo    стена источник постов
     * @param lastPostDate последний загруженный пост на клиенте, null если запрос первой стены
     * @param timeStep     дельта времени, не null при запросе первой стены от клиента
     * @param offset       отступ для vk api
     * @return 1)  Запрос первой стены: все посты по времени от первого незакрепленного date до (date - timeStep)
     * 2)  Посты до определенной даты: все посты между offset и lastPostDate
     * @throws ClientException todo
     * @throws ApiException todo
     */
    private VkWallResponse getPostsFromWall(@NotNull OwnerInfo ownerInfo, @Nullable Integer lastPostDate,
                                            @Nullable Integer timeStep, @NotNull Integer offset, @NotNull Boolean isFirstWall) throws ClientException, ApiException {
        VkWallResponse result = new VkWallResponse(
                (Application.getVk().wall().getExtended(Application.getServiceActor())
                        .ownerId(ownerInfo.ownerId)
                        .count(postCount)
                        .offset(offset)
                        .execute()));
        if (result.resultList.size() == 0)
            return result;
        if (isFirstWall)
            lastPostDate = (result.resultList.get(0).getIsPinned() == null && result.resultList.size() > 1) ?
                    result.resultList.get(0).getDate() - timeStep :
                    result.resultList.get(1).getDate() - timeStep;
        if (result.resultList.get(result.resultList.size() - 1).getDate() > lastPostDate && result.resultList.size() == postCount) {
            VkWallResponse recursiveResult = /*isFirstWall ?
                    getFirstPosts(ownerInfo, timeStep, offset + result.resultList.size()) :*/
                    getAfterLastPosts(ownerInfo, lastPostDate, offset + result.resultList.size());
            result.resultList.addAll(recursiveResult.resultList);
            result.count = result.count + recursiveResult.count;
            return result;
        }
        Integer finalLastPostDate = lastPostDate;
        result.resultList.removeIf(wallPostFull -> wallPostFull.getDate() < finalLastPostDate);
        return result;
    }

    /**
     * Объект - ответ от одной стены
     */
    private static class VkWallResponse {
        private List<WallPostFull> resultList = new ArrayList<>();
        private Integer count;
        private Set<UserFull> profiles = new HashSet<>();
        private Set<GroupFull> groups = new HashSet<>();

        VkWallResponse(GetExtendedResponse response) {
            this.resultList.addAll(response.getItems());
            this.count = response.getCount();
            if (response.getProfiles() != null)
                profiles.addAll(response.getProfiles());
            if (response.getGroups() != null)
                groups.addAll(response.getGroups());
        }
    }

    public List<OwnerInfo> getOwnerInfoList() {
        return ownerInfoList;
    }

    public List<WallPostFull> getWallPostList() {
        return wallPostList;
    }

    public Set<UserFull> getProfiles() {
        return profiles;
    }

    public Set<GroupFull> getGroups() {
        return groups;
    }

}
