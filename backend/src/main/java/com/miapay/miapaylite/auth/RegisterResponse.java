package com.miapay.miapaylite.auth;

import java.util.UUID;

public record RegisterResponse(UUID id, String email, Role role) {
}
