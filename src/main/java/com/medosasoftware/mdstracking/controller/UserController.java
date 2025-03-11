package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @PostMapping("/{userId}/assign-company/{companyId}")
    public String assignCompany(@PathVariable Long userId, @PathVariable Long companyId) {
        userService.assignCompanyToUser(userId, companyId);
        return "Company assigned to user successfully";
    }

    @GetMapping("/{email}")
    public Optional<User> getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email);
    }
}
