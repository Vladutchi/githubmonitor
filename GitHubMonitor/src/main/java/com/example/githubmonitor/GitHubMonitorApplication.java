package com.example.githubmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GitHubMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(GitHubMonitorApplication.class, args);
    }
}