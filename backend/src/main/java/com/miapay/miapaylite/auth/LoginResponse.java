package com.miapay.miapaylite.auth;

// expiresIn is expressed in seconds, like the OAuth 2.0 token response.
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
