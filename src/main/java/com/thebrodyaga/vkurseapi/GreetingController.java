package com.thebrodyaga.vkurseapi;

import com.sun.istack.internal.Nullable;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.wall.WallPostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.thebrodyaga.vkurseapi.model.VkPostSourceInfo;
import com.thebrodyaga.vkurseapi.model.VkWallToClient;
import com.thebrodyaga.vkurseapi.model.VkWallsFromClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.thebrodyaga.vkurseapi.Application.actor;
import static com.thebrodyaga.vkurseapi.Application.vk;

@ResponseBody
@RestController
public class GreetingController {
    public static int timeStep = 60 * 60 * 2; //2 часа,
    public static int postCount = 20; //кол-во постов,

    @PostMapping("/wallPostById")
    public VkWallToClient wallPostById(@RequestBody VkWallsFromClient body) {
        VkWallToClient answer = new VkWallToClient();
        ExecutorService service = Executors.newCachedThreadPool();
        for (VkPostSourceInfo source : body.wallsFromClient) {//список необходимых постов
            if (source.ownerId == null)
                continue;
            service.submit(() -> {
                try {
                    addVkWallResult(body, answer, source, getWallPost(source,
                            body.lastPostDate,
                            source.offset));
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
        answer.vkPostFullList.sort((o1, o2) -> o2.getDate().compareTo(
                o1.getDate()));
        if (body.lastPostDate != null)
            return answer;

        Integer lastPostDate = answer.vkPostFullList.get(0).getDate() - timeStep;
        answer.vkPostFullList.removeIf(wallPostFull -> wallPostFull.getDate() < lastPostDate);
        for (VkPostSourceInfo sourceResult : answer.vkPostSourceResult)
            sourceResult.offset = Math.toIntExact(answer.vkPostFullList
                    .stream()
                    .filter(wallPostFull
                            -> wallPostFull.getOwnerId().equals(sourceResult.ownerId))
                    .count());
        return answer;
    }

    /**
     * Синхронный метод обработки результата от каждого источника постов.
     * Один ownerId - один поток
     */
    private synchronized void addVkWallResult(VkWallsFromClient body, VkWallToClient answer,
                                              VkPostSourceInfo source, VkWallResponse currentOwnerIdPosts) {
        answer.vkPostFullList.addAll(currentOwnerIdPosts.resultList);
        answer.vkPostSourceResult.add(new VkPostSourceInfo(source.ownerId,
                body.lastPostDate != null ? //если запрос первый, то offset проставляется позже
                        currentOwnerIdPosts.resultList.size()
                                + (source.offset != null ? source.offset : 0) : 0,
                currentOwnerIdPosts.count));
    }


    /**
     * @param wall         обьект источника постов
     * @param lastPostDate время последнего возможного поста
     * @param offset       отступ, меняется при рекурсии
     * @return список постов в диапазоне свежее lastPostDate,
     * if (lastPostDate == null) lastPostDate = get(0).getDate()(самый новый) - timeStep(дельта времени постов);
     * @throws ClientException //todo добавить обработку
     * @throws ApiException    //todo добавить обработку
     */
    private VkWallResponse getWallPost(VkPostSourceInfo wall,
                                       @Nullable Integer lastPostDate,
                                       int offset) throws ClientException, ApiException {
        VkWallResponse result = new VkWallResponse(
                (vk.wall().get(actor)
                        .ownerId(wall.ownerId)
                        .count(postCount)
                        .offset(offset)
                        .execute()));
        if (lastPostDate == null)
            lastPostDate = result.resultList.get(0).getIsPinned() != null ?
                    result.resultList.get(0).getDate() - timeStep :
                    result.resultList.get(1).getDate() - timeStep;
        if (result.resultList.get(result.resultList.size() - 1).getDate() > lastPostDate) {
            //  все полученные посты включены в временной диапазон, нужно проверить если ли ещё посты в диапазоне
            VkWallResponse recursiveResult = getWallPost(wall, lastPostDate, offset + postCount);
            result.resultList.addAll(recursiveResult.resultList);
            result.count = recursiveResult.count;
            return result;
        }
        Integer timeFilter = lastPostDate;
        //  отчистка более поздних постов
        result.resultList.removeIf(wallPostFull -> wallPostFull.getDate() < timeFilter);
        return result;
    }

    /**
     * Объект - ответ от одной стены
     * resultList - посты в timeStep(дельта времени постов)
     * count - колличество постов на стене. Сравнивать на клиенте, при != удалять дубли
     */
    private static class VkWallResponse {
        List<WallPostFull> resultList = new ArrayList<>();
        Integer count;

        VkWallResponse(GetResponse response) {
            this.resultList.addAll(response.getItems());
            this.count = response.getCount();
        }
    }
}
