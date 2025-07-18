package com.habitforge.habitforge_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LandingController {

    @GetMapping("/")
    public String home() {
        return "Habit Forge Backend is running!";
    }
}

