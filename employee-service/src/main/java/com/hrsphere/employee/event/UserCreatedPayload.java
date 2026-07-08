package com.hrsphere.employee.event;

import java.util.List;

// Consumer-owned copy of the event payload contract; do not share payload DTOs across services.
public record UserCreatedPayload(String username, String email, List<String> roles) {}
