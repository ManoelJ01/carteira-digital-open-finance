package com.manoel.carteiradigital.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String gerarToken(UserDetails userDetails) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(secretKey)
                .compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String extrairUsername(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean tokenValido(String token, UserDetails userDetails) {
        String username = extrairUsername(token);
        return username.equals(userDetails.getUsername()) && !tokenExpirado(token);
    }

    private boolean tokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
