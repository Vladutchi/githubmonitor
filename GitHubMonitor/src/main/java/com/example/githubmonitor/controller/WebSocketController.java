package com.example.githubmonitor.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/send-repo")
    @SendTo("/topic/updates")
    public String sendRepoUpdate(String repoUrl) {
        System.out.println("WebSocket received: " + repoUrl);
        return "Repository update received for: " + repoUrl + " at " + System.currentTimeMillis();
    }
}