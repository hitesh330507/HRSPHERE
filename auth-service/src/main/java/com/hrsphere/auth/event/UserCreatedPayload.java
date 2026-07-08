package com.hrsphere.auth.event;

import java.util.List;

public record UserCreatedPayload(String username, String email, List<String> roles) {}
