package com.hivpn.server.service;

import com.hivpn.server.model.User;
import com.hivpn.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public CompletableFuture<User> getOrCreateUserByTelegramId(Long telegramId, String username) {
        return userRepository.findByTelegramId(telegramId)
                .thenCompose(userOpt -> {
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setLastLoginAt(Instant.now().toString());
                        
                        if (user.getAccessToken() == null) {
                            String accessToken = generateAccessToken();
                            log.info("Generated new access token for existing user: {}", accessToken);
                            user.setAccessToken(accessToken);
                        }
                        
                        log.info("Existing user found: {}", user);
                        return userRepository.save(user);
                    }
                    
                    User newUser = new User();
                    String accessToken = generateAccessToken();
                    log.info("Generated new access token: {}", accessToken);
                    
                    newUser.setId(UUID.randomUUID().toString());
                    newUser.setUsername(username);
                    newUser.setTelegramId(telegramId);
                    newUser.setCreatedAt(Instant.now().toString());
                    newUser.setLastLoginAt(Instant.now().toString());
                    newUser.setPremium(false);
                    newUser.setStatus("active");
                    newUser.setAccessToken(accessToken);
                    
                    log.info("Creating new user: {}", newUser);
                    return userRepository.save(newUser);
                });
    }

    public CompletableFuture<Optional<User>> findByAccessToken(String accessToken) {
        log.info("Searching for user with access token: {}", accessToken);
        return userRepository.findByAccessToken(accessToken);
    }

    public CompletableFuture<Void> deleteUser(User user) {
        return userRepository.deleteUser(user);
    }

    private String generateAccessToken() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("Generated access token: {}", token);
        return token;
    }
} 