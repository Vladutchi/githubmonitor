package com.example.githubmonitor.scheduler;

import com.example.githubmonitor.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

@Component
public class GitHubScheduler {

    private final Map<String, Map<String, Object>> monitoredRepos = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GitHubService gitHubApiService;

    public void addRepository(String repoUrl) {
        Map<String, Object> repoInfo = gitHubApiService.getRepositoryInfo(repoUrl);
        if (repoInfo.containsKey("success")) {
            monitoredRepos.put(repoUrl, repoInfo);
            messagingTemplate.convertAndSend("/topic/updates",
                    "✅ Now monitoring: " + repoInfo.get("fullName"));
        }
    }

    @Scheduled(fixedDelay = 60000) // Check every 60 seconds
    public void checkForUpdates() {
        if (monitoredRepos.isEmpty()) {
            return;
        }

        monitoredRepos.forEach((repoUrl, oldInfo) -> {
            try {
                Map<String, Object> newInfo = gitHubApiService.getRepositoryInfo(repoUrl);

                if (newInfo.containsKey("success")) {
                    // Check for changes
                    checkAndNotifyChanges(repoUrl, oldInfo, newInfo);

                    // Update stored info
                    monitoredRepos.put(repoUrl, newInfo);
                }
            } catch (Exception e) {
                System.err.println("Error checking updates for " + repoUrl + ": " + e.getMessage());
            }
        });
    }

    private void checkAndNotifyChanges(String repoUrl, Map<String, Object> oldInfo, Map<String, Object> newInfo) {
        String repoName = (String) newInfo.get("fullName");

        // Check stars
        int oldStars = (int) oldInfo.get("stars");
        int newStars = (int) newInfo.get("stars");
        if (newStars != oldStars) {
            String message = String.format("⭐ %s: Stars changed from %d to %d",
                    repoName, oldStars, newStars);
            messagingTemplate.convertAndSend("/topic/updates", message);
        }

        // Check forks
        int oldForks = (int) oldInfo.get("forks");
        int newForks = (int) newInfo.get("forks");
        if (newForks != oldForks) {
            String message = String.format("🍴 %s: Forks changed from %d to %d",
                    repoName, oldForks, newForks);
            messagingTemplate.convertAndSend("/topic/updates", message);
        }

        // Check issues
        int oldIssues = (int) oldInfo.get("openIssues");
        int newIssues = (int) newInfo.get("openIssues");
        if (newIssues != oldIssues) {
            String message = String.format("🐛 %s: Issues changed from %d to %d",
                    repoName, oldIssues, newIssues);
            messagingTemplate.convertAndSend("/topic/updates", message);
        }

        // Check update time
        String oldUpdate = (String) oldInfo.get("updatedAt");
        String newUpdate = (String) newInfo.get("updatedAt");
        if (!newUpdate.equals(oldUpdate)) {
            String message = String.format("📅 %s: Repository was updated at %s",
                    repoName, newUpdate);
            messagingTemplate.convertAndSend("/topic/updates", message);
        }
    }

    public int getMonitoredCount() {
        return monitoredRepos.size();
    }
}