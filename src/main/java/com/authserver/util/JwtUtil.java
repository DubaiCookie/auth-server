package com.authserver.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.authserver.entity.User;
import com.authserver.exception.InvalidTokenException;
import com.authserver.exception.ExpiredTokenException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:RANDOM_SECRET_KEY}")
    private String secret;

    @Value("${jwt.issuer:simple-auth-server}")
    private String issuer;

    @Value("${jwt.access-token-expiration-minutes:60}")
    private int accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-days:30}")
    private int refreshTokenExpirationDays;

    /**
     * Access Token 생성
     */
    public String createAccessToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES)))
                .withSubject(String.valueOf(user.getId()))
                .withClaim("username", user.getUsername())
                .withClaim("type", "access")
                .sign(algorithm);
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS)))
                .withSubject(String.valueOf(user.getId()))
                .withClaim("type", "refresh")
                .sign(algorithm);
    }

    /**
     * Refresh Token 만료 시간 반환
     */
    public LocalDateTime getRefreshTokenExpiresAt() {
        return LocalDateTime.now().plusDays(refreshTokenExpirationDays);
    }

    /**
     * 토큰 검증
     */
    public void validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
            verifier.verify(token);
        } catch (TokenExpiredException e) {
            throw new ExpiredTokenException("Token has expired");
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Invalid token");
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return Long.parseLong(jwt.getSubject());
        } catch (Exception e) {
            throw new InvalidTokenException("Failed to extract user ID from token");
        }
    }

    /**
     * 토큰 타입 추출
     */
    public String getTokenType(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("type").asString();
        } catch (Exception e) {
            throw new InvalidTokenException("Failed to extract token type");
        }
    }
}
