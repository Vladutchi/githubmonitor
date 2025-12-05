package com.example.githubmonitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Enter a GitHub repository URL");
        return "index";
    }

    @PostMapping("/submit-repo")
    public String submitRepo(@RequestParam String repoUrl, Model model) {
        System.out.println("Received repository URL: " + repoUrl);
        model.addAttribute("message", "Processing repository: " + repoUrl);
        model.addAttribute("repoUrl", repoUrl);
        return "index";
    }
}