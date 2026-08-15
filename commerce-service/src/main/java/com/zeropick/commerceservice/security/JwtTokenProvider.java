package com.zeropick.commerceservice.security;

import com.zeropick.commerceservice.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String encodedSecret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.signingKey = encodedSecret.isBlank()
                ? Jwts.SIG.HS256.key().build()
                : Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedSecret));
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero.");
        }
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(Member member) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(member.getId().toString())
                .claim("memberId", member.getId())
                .claim("name", member.getName())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
