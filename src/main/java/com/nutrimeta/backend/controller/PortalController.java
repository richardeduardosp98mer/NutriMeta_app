package com.nutrimeta.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/portal")
public class PortalController {

    @GetMapping
    public String panel(){
        return "portal/dashboard";
    }

    @GetMapping("/login")
    public String login(){
        return "portal/login";
    }
}
