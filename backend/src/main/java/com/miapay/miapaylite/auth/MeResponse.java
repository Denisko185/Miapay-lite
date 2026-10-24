package com.miapay.miapaylite.auth;

import java.util.UUID;

// Never exposes the password hash. The balance is in integer FCFA.
public record MeResponse(UUID id, String email, Role role, long balance) {
}
