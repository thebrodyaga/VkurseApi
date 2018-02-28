package com.thebrodyaga.vkurseapi;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    private static UserActor actor;
    private static VkApiClient vk;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        TransportClient transportClient = HttpTransportClient.getInstance();
        vk = new VkApiClient(transportClient);
        actor = new UserActor(Constants.USER_ID, Constants.ACCESS_TOKEN);
    }

    public static UserActor getActor() {
        return actor;
    }

    public static VkApiClient getVk() {
        return vk;
    }
}