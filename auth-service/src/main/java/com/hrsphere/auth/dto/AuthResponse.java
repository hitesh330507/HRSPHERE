package com.hrsphere.auth.dto;

import java.util.List;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long accessTokenExpiresIn,
    String username,
    String email,
    List<String> roles) {}
