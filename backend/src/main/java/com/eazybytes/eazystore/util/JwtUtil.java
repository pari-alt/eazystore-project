package com.eazybytes.eazystore.util;

import com.eazybytes.eazystore.constants.ApplicationConstants;
import com.eazybytes.eazystore.entity.Customer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final Environment env;

    // Normal username/password login
    public String generateJwtToken(Authentication authentication) {

        String secret = env.getProperty(
                ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE
        );

        SecretKey secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        Customer fetchedCustomer =
                (Customer) authentication.getPrincipal();

        return Jwts.builder()
                .issuer("Eazy Store")
                .subject("JWT Token")
                .claim("username", fetchedCustomer.getName())
                .claim("email", fetchedCustomer.getEmail())
                .claim("mobileNumber", fetchedCustomer.getMobileNumber())
                .claim(
                        "roles",
                        authentication.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(","))
                )
                .issuedAt(new java.util.Date())
                .expiration(
                        new java.util.Date(
                                System.currentTimeMillis()
                                        + 24 * 60 * 60 * 1000
                        )
                )
                .signWith(secretKey)
                .compact();
    }

    // Google OAuth2 login
    public String generateJwtTokenForCustomer(Customer customer) {

        String secret = env.getProperty(
                ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE
        );

        SecretKey secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        String roles = customer.getRoles()
                .stream()
                .map(role -> role.getName())
                .map(role ->
                        role.startsWith("ROLE_")
                                ? role
                                : "ROLE_" + role
                )
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .issuer("Eazy Store")
                .subject("JWT Token")
                .claim("username", customer.getName())
                .claim("email", customer.getEmail())
                .claim("mobileNumber", customer.getMobileNumber())
                .claim("roles", roles)
                .issuedAt(new java.util.Date())
                .expiration(
                        new java.util.Date(
                                System.currentTimeMillis()
                                        + 24 * 60 * 60 * 1000
                        )
                )
                .signWith(secretKey)
                .compact();
    }
}