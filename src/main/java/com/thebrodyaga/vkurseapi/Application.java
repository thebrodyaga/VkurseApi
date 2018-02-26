package com.thebrodyaga.vkurseapi;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.thebrodyaga.vkurseapi.Constants.APP_ID;
import static com.thebrodyaga.vkurseapi.Constants.CLIENT_SECRET;

@SpringBootApplication
public class Application {

    public static ServiceActor actor;
    public static VkApiClient vk;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        TransportClient transportClient = HttpTransportClient.getInstance();
        vk = new VkApiClient(transportClient);
        actor = new ServiceActor(APP_ID, CLIENT_SECRET);
    }
}