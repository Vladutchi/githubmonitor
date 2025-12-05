package com.example.githubmonitor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GitHubService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getRepositoryInfo(String repoUrl) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Parse owner and repo from URL
            String[] parts = repoUrl.replace("https://github.com/", "")
                    .replace("http://github.com/", "")
                    .split("/");
            if (parts.length < 2) {
                result.put("error", "Invalid GitHub repository URL format");
                return result;
            }

            String owner = parts[0];
            String repo = parts[1].replace(".git", "");

            // GitHub API endpoint
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo;

            // Add headers (GitHub requires User-Agent)
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "GitHubMonitor-App");
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // Extract repository information
                result.put("name", root.path("name").asText());
                result.put("fullName", root.path("full_name").asText());
                result.put("description", root.path("description").asText());
                result.put("stars", root.path("stargazers_count").asInt());
                result.put("forks", root.path("forks_count").asInt());
                result.put("watchers", root.path("watchers_count").asInt());
                result.put("openIssues", root.path("open_issues_count").asInt());
                result.put("language", root.path("language").asText());
                result.put("createdAt", root.path("created_at").asText());
                result.put("updatedAt", root.path("updated_at").asText());
                result.put("url", root.path("html_url").asText());
                result.put("size", root.path("size").asInt());
                result.put("license", root.path("license").path("name").asText("No license"));
                result.put("success", true);
            } else {
                result.put("error", "GitHub API returned: " + response.getStatusCode());
            }

        } catch (Exception e) {
            result.put("error", "Failed to fetch repository: " + e.getMessage());
        }

        return result;
    }
}