package com.thebrodyaga.vkurseapi;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.thebrodyaga.vkurseapi.Constants.*;

@SpringBootApplication
public class Application {

    private static ServiceActor serviceActor;
    private static ServiceActor serviceSecretActor;
    private static UserActor userActor;
    private static VkApiClient vk;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        TransportClient transportClient = HttpTransportClient.getInstance();
        vk = new VkApiClient(transportClient);
        userActor = new UserActor(USER_ID, ACCESS_TOKEN);
        serviceActor = new ServiceActor(APP_ID, CLIENT_TOKEN);
        serviceSecretActor = new ServiceActor(APP_ID, SECRET_KEY, CLIENT_TOKEN);
        printIp();
    }

    private static void printIp() {
        InetAddress ip;
        try {
            ip = InetAddress.getLocalHost();
            System.out.println("Current IP address : " + ip.getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static ServiceActor getServiceActor() {
        return serviceActor;
    }

    public static ServiceActor getServiceSecretActor() {
        return serviceSecretActor;
    }

    public static UserActor getUserActor() {
        return userActor;
    }

    public static VkApiClient getVk() {
        return vk;
    }
}