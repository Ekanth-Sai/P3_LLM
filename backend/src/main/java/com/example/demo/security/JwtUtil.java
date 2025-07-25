package com.example.demo.security;

import java.util.Date;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final String SECRET = "PASSWORD"; //add to .env later

    public String generateToken(String email, String role) {
        return Jwts.builder().setSubject(email).claim("role", role).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SignatureAlgorithm.HS256, SECRET).compact();
    }
}
