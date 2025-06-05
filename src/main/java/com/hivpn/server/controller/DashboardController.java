package com.hivpn.server.controller;

import com.hivpn.server.model.User;
import com.hivpn.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final UserService userService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/profile/{accessToken}")
    public String profile(
            @PathVariable String accessToken,
            @CookieValue(value = "access_token", required = false) String cookieToken,
            Model model) {
        
        String token = accessToken != null ? accessToken : cookieToken;
        
        if (token == null) {
            return "error";
        }

        try {
            CompletableFuture<Optional<User>> userFuture = userService.findByAccessToken(token);
            Optional<User> userOpt = userFuture.get(5, TimeUnit.SECONDS);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("Found user for profile: {}", user);
                model.addAttribute("user", user);
                return "dashboard";
            }
            
            log.warn("No user found for access token: {}", token);
            return "error";
        } catch (Exception e) {
            log.error("Error while getting user profile", e);
            return "error";
        }
    }

    public String dashboard(
            @PathVariable(required = false) String accessToken,
            @CookieValue(value = "access_token", required = false) String cookieToken,
            Model model) {
        
        String token = accessToken != null ? accessToken : cookieToken;
        
        if (token == null) {
            return "error";
        }

        try {
            CompletableFuture<Optional<User>> userFuture = userService.findByAccessToken(token);
            Optional<User> userOpt = userFuture.get(5, TimeUnit.SECONDS);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("Found user for dashboard: {}", user);
                model.addAttribute("user", user);
                return "dashboard";
            }
            
            log.warn("No user found for access token: {}", token);
            return "error";
        } catch (Exception e) {
            log.error("Error while getting user dashboard", e);
            return "error";
        }
    }

    @PostMapping("/api/delete-account/{accessToken}")
    @ResponseBody
    public ResponseEntity<?> deleteAccount(@PathVariable String accessToken) {
        log.info("Attempting to delete account for token: {}", accessToken);
        
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("No access token provided for account deletion");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            CompletableFuture<Optional<User>> userFuture = userService.findByAccessToken(accessToken);
            Optional<User> userOpt = userFuture.get(5, TimeUnit.SECONDS);
            
            if (userOpt.isEmpty()) {
                log.warn("User not found for token: {}", accessToken);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
            }

            User user = userOpt.get();
            log.info("Found user for deletion: {}", user);
            
            CompletableFuture<Void> deleteFuture = userService.deleteUser(user);
            deleteFuture.get(5, TimeUnit.SECONDS);
            
            log.info("Successfully deleted user: {}", user.getId());
            return ResponseEntity.ok()
                    .body(Map.of("success", true, "message", "Account deleted successfully"));
                    
        } catch (Exception e) {
            log.error("Error deleting user account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error deleting account: " + e.getMessage()));
        }
    }

    @PostMapping("/api/generate-config/{accessToken}")
    @ResponseBody
    public ResponseEntity<?> generateConfig(@PathVariable String accessToken) {
        log.info("Attempting to generate config for token: {}", accessToken);
        
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("No access token provided for config generation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            CompletableFuture<Optional<User>> userFuture = userService.findByAccessToken(accessToken);
            Optional<User> userOpt = userFuture.get(5, TimeUnit.SECONDS);
            
            if (userOpt.isEmpty()) {
                log.warn("User not found for token: {}", accessToken);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
            }

            User user = userOpt.get();
            log.info("Generating config for user: {}", user.getId());
            
            String configUrl = String.format("https://hivpn.example.com/config/%s/%s", 
                user.getId(), 
                user.getAccessToken());
            
            return ResponseEntity.ok()
                    .body(Map.of(
                        "success", true,
                        "configUrl", configUrl
                    ));
                    
        } catch (Exception e) {
            log.error("Error generating config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error generating config: " + e.getMessage()));
        }
    }

    @PostMapping("/api/verify-config-url")
    public ResponseEntity<?> verifyConfigUrl(@RequestBody Map<String, String> request) {
        String configUrl = request.get("config_url");
        if (configUrl == null || configUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Config URL is required"
            ));
        }

        try {
            String[] parts = configUrl.split("/");
            if (parts.length < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid config URL format"
                ));
            }

            String userId = parts[parts.length - 2];
            String accessToken = parts[parts.length - 1];

            CompletableFuture<Optional<User>> userFuture = userService.findByAccessToken(accessToken);
            Optional<User> userOpt = userFuture.get(5, TimeUnit.SECONDS);
            
            if (userOpt.isEmpty() || !userOpt.get().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Invalid config URL"
                ));
            }

            User user = userOpt.get();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "user_id", user.getAccessToken(),
                "access_token", user.getAccessToken()
            ));

        } catch (Exception e) {
            log.error("Error verifying config URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error verifying config URL"
            ));
        }
    }
} 
