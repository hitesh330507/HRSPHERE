package com.hrsphere.auth.dto;

import java.time.Instant;
import java.util.List;

public record AuthResponse(
    String username, String email, List<String> roles, String message, Instant timestamp) {}
