package com.example.githubmonitor.controller;

import com.example.githubmonitor.service.GitHubService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GitHubService gitHubApiService;

    @MessageMapping("/send-repo")
    @SendTo("/topic/updates")
    public String sendRepoUpdate(String repoUrl) {
        System.out.println("WebSocket received: " + repoUrl);

        // Validate URL
        if (!repoUrl.contains("github.com")) {
            return "Error: Please enter a valid GitHub repository URL";
        }

        // Fetch repository info from GitHub API
        Map<String, Object> repoInfo = gitHubApiService.getRepositoryInfo(repoUrl);

        if (repoInfo.containsKey("error")) {
            return "Error: " + repoInfo.get("error");
        }

        // Format the response
        StringBuilder response = new StringBuilder();
        response.append("✅ Repository: ").append(repoInfo.get("fullName")).append("\n");

        if (repoInfo.get("description") != null && !repoInfo.get("description").toString().isEmpty()) {
            response.append("📝 Description: ").append(repoInfo.get("description")).append("\n");
        }

        response.append("⭐ Stars: ").append(repoInfo.get("stars"))
                .append(" | 🍴 Forks: ").append(repoInfo.get("forks"))
                .append(" | 🐛 Issues: ").append(repoInfo.get("openIssues")).append("\n");

        if (repoInfo.get("language") != null && !repoInfo.get("language").toString().isEmpty()) {
            response.append("💻 Language: ").append(repoInfo.get("language")).append("\n");
        }

        response.append("📅 Last Updated: ").append(repoInfo.get("updatedAt")).append("\n");
        response.append("🔗 URL: ").append(repoInfo.get("url"));

        return response.toString();
    }
}