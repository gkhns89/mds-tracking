package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String email,
                                               @RequestParam String username,
                                               @RequestParam String password,
                                               @RequestParam String companyName) {
        try {
            userService.registerUser(email, username, password, companyName);
            return ResponseEntity.ok("Kullanıcı başarıyla oluşturuldu!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.findByEmail(email);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
