package com.example.demo.service;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.SignatureAlgorithm;
import com.example.demo.model.User;
import java.util.Date;

import io.jsonwebtoken.Jwts;

@Service
public class JwtService {
    private static final String SECRET = "secretkey123";
    private static final long EXPIRATION = 86400000;

    public String generateToken(User user) {
        return Jwts.builder().setSubject(user.getEmail()).claim("role", user.getRole()).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET).compact();
    }
}
