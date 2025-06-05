package com.hivpn.server.model;

import lombok.Data;
import java.time.Instant;

@Data
public class User {
    private String id;
    private String username;
    private Long telegramId;
    private String createdAt;
    private String lastLoginAt;
    private boolean premium;
    private String status;
    private String accessToken;
} 