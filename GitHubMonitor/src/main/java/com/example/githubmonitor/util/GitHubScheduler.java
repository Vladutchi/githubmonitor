package com.example.githubmonitor.util;

import com.example.githubmonitor.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class GitHubScheduler {

    // Store monitored repositories per session
    private final Map<String, Map<String, Map<String, Object>>> sessionMonitoredRepos = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GitHubService gitHubApiService;

    public void addRepositoryForSession(String sessionId, String repoUrl) {
        Map<String, Object> repoInfo = gitHubApiService.getRepositoryInfo(repoUrl);
        if (repoInfo.containsKey("success")) {
            // Get or create session entry
            Map<String, Map<String, Object>> sessionRepos = sessionMonitoredRepos
                    .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());

            sessionRepos.put(repoUrl, repoInfo);

            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/updates",
                    "✅ Now monitoring in this session: " + repoInfo.get("fullName")
            );
        }
    }

    public void removeSession(String sessionId) {
        sessionMonitoredRepos.remove(sessionId);
    }

    public void removeRepositoryForSession(String sessionId, String repoUrl) {
        Map<String, Map<String, Object>> sessionRepos = sessionMonitoredRepos.get(sessionId);
        if (sessionRepos != null) {
            sessionRepos.remove(repoUrl);
            if (sessionRepos.isEmpty()) {
                sessionMonitoredRepos.remove(sessionId);
            }
        }
    }

    @Scheduled(fixedDelay = 60000) // Check every 60 seconds
    public void checkForUpdates() {
        if (sessionMonitoredRepos.isEmpty()) {
            return;
        }

        // Check each session's repositories
        sessionMonitoredRepos.forEach((sessionId, sessionRepos) -> {
            sessionRepos.forEach((repoUrl, oldInfo) -> {
                try {
                    Map<String, Object> newInfo = gitHubApiService.getRepositoryInfo(repoUrl);

                    if (newInfo.containsKey("success")) {
                        // Check for changes
                        checkAndNotifySessionChanges(sessionId, repoUrl, oldInfo, newInfo);

                        // Update stored info
                        sessionRepos.put(repoUrl, newInfo);
                    }
                } catch (Exception e) {
                    System.err.println("Error checking updates for " + repoUrl + " in session " + sessionId + ": " + e.getMessage());
                }
            });
        });
    }

    private void checkAndNotifySessionChanges(String sessionId, String repoUrl,
                                              Map<String, Object> oldInfo,
                                              Map<String, Object> newInfo) {
        String repoName = (String) newInfo.get("fullName");

        // Check stars
        int oldStars = (int) oldInfo.get("stars");
        int newStars = (int) newInfo.get("stars");
        if (newStars != oldStars) {
            String message = String.format("⭐ %s: Stars changed from %d to %d (Session update)",
                    repoName, oldStars, newStars);
            sendToSession(sessionId, message);
        }

        // Check forks
        int oldForks = (int) oldInfo.get("forks");
        int newForks = (int) newInfo.get("forks");
        if (newForks != oldForks) {
            String message = String.format("🍴 %s: Forks changed from %d to %d (Session update)",
                    repoName, oldForks, newForks);
            sendToSession(sessionId, message);
        }

        // Check issues
        int oldIssues = (int) oldInfo.get("openIssues");
        int newIssues = (int) newInfo.get("openIssues");
        if (newIssues != oldIssues) {
            String message = String.format("🐛 %s: Issues changed from %d to %d (Session update)",
                    repoName, oldIssues, newIssues);
            sendToSession(sessionId, message);
        }

        // Check update time
        String oldUpdate = (String) oldInfo.get("updatedAt");
        String newUpdate = (String) newInfo.get("updatedAt");
        if (!newUpdate.equals(oldUpdate)) {
            String message = String.format("📅 %s: Repository was updated at %s (Session update)",
                    repoName, newUpdate);
            sendToSession(sessionId, message);
        }
    }

    private void sendToSession(String sessionId, String message) {
        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/updates",
                message
        );
    }

    public int getSessionCount() {
        return sessionMonitoredRepos.size();
    }

    public int getTotalMonitoredCount() {
        return sessionMonitoredRepos.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}