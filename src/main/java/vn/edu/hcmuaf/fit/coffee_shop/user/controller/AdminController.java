package vn.edu.hcmuaf.fit.coffee_shop.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @GetMapping("/hello")
    public String adminHello() {
        return "Hello ADMIN only!";
    }
}