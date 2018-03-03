package com.thebrodyaga.vkurseapi.model;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.thebrodyaga.vkurseapi.Application;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.wall.WallPostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.thebrodyaga.vkurseapi.controller.VkController.postCount;
import static com.thebrodyaga.vkurseapi.controller.VkController.timeStep;

public class VkWall {
    private List<OwnerInfo> ownerInfoList = new ArrayList<>();
    private List<WallPostFull> wallPostList = new ArrayList<>();

    public VkWall getFirstWall(GetVkWallBody firstWallBody) {
        return getWall(firstWallBody, true);
    }

    public VkWall getWallAfterLast(GetVkWallBody afterLastBody) {
        return getWall(afterLastBody, false);
    }

    /**
     * vkWallBody.lastPostDate     обязательный параметр при isFirstWall == false
     * vkWallBody.timeStep         при null vkWallBody.timeStep = VkController.timeStep
     * if (isFirstWall==true)
     * получаю посты от каждого ownerId в промежутке от первого незакрепленного date до (date - timeStep)
     * сортирую по дате, удаляю от первого date до (date - timeStep), проставляю offset в оставшихся
     * else
     * получаю готовые посты в промежутке, сортирую результат
     */
    private VkWall getWall(GetVkWallBody vkWallBody, @NotNull Boolean isFirstWall) {
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
                    VkWallResponse currentResponse = isFirstWall ?
                            getFirstPosts(source, vkWallBody.timeStep, source.offset) :
                            getAfterLastPosts(source, vkWallBody.lastPostDate - vkWallBody.timeStep, source.offset);
                    synchronized (VkWall.this) {
                        wallPostList.addAll(currentResponse.resultList);
                        ownerInfoList.add(new OwnerInfo(source.ownerId,
                                isFirstWall ? 0 :
                                        currentResponse.resultList.size()
                                                + (source.offset != null ? source.offset : 0),
                                currentResponse.count));
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
        if (!isFirstWall)
            this.wallPostList.removeIf(wallPostFull -> wallPostFull.getDate() > vkWallBody.lastPostDate);
        this.wallPostList.sort((o1, o2) -> o2.getDate().compareTo(
                o1.getDate()));
        System.out.println();
        if (!isFirstWall)
            return this;
        Integer lastPostDate = wallPostList.get(0).getDate() - vkWallBody.timeStep;
        wallPostList.removeIf(wallPostFull -> wallPostFull.getDate() < lastPostDate);
        for (OwnerInfo sourceResult : ownerInfoList)
            sourceResult.offset = Math.toIntExact(wallPostList
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

    /**
     * @param ownerInfo    стена источник постов
     * @param lastPostDate последний загруженный пост на клиенте, null если запрос первой стены
     * @param timeStep     дельта времени, не null при запросе первой стены от клиента
     * @param offset       отступ для vk api
     * @return 1)  Запрос первой стены: все посты по времени от первого незакрепленного date до (date - timeStep)
     * 2)  Посты до определенной даты: все посты между offset и lastPostDate
     * @throws ClientException
     * @throws ApiException
     */
    private VkWallResponse getPostsFromWall(@NotNull OwnerInfo ownerInfo, @Nullable Integer lastPostDate,
                                            @Nullable Integer timeStep, @NotNull Integer offset, @NotNull Boolean isFirstWall) throws ClientException, ApiException {
        VkWallResponse result = new VkWallResponse(
                (Application.getVk().wall().get(Application.getServiceActor())
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

        VkWallResponse(GetResponse response) {
            this.resultList.addAll(response.getItems());
            this.count = response.getCount();
        }
    }

    public List<OwnerInfo> getOwnerInfoList() {
        return ownerInfoList;
    }

    public List<WallPostFull> getWallPostList() {
        return wallPostList;
    }
}
