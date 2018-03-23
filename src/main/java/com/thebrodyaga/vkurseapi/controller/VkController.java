package com.thebrodyaga.vkurseapi.controller;

import com.thebrodyaga.vkurseapi.model.GetVkWallBody;
import com.thebrodyaga.vkurseapi.model.VkWall;
import org.springframework.web.bind.annotation.*;

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
}
