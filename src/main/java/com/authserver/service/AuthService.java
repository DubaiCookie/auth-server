package com.authserver.service;

import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.authserver.entity.User;
import com.authserver.repository.UserRepository;
import com.authserver.util.JwtUtil;
import com.authserver.exception.InvalidTokenException;
import com.authserver.exception.ExpiredTokenException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * 회원가입
     */
    @Transactional
    public User signUp(String username, String rawPassword) {
        // 중복 체크
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists");
        }

        // 비밀번호 해시
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));

        // 사용자 저장
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * 로그인
     */
    public User login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            return null; // 사용자 없음
        }

        // 해시 검증
        if (!BCrypt.checkpw(rawPassword, user.getPassword())) {
            return null; // 비밀번호 불일치
        }

        return user; // 로그인 성공
    }

    /**
     * Refresh Token 저장
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        long expirationMillis = jwtUtil.getRefreshTokenExpiresAt().atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli() - System.currentTimeMillis();
        refreshTokenService.saveRefreshToken(userId, refreshToken, expirationMillis);
    }

    /**
     * Refresh Token으로 Access Token 갱신
     */
    public User refreshAccessToken(String refreshToken) {
        // 1. JWT 자체 검증
        jwtUtil.validateToken(refreshToken);

        // 2. 토큰 타입 확인
        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        // 3. Redis에 저장된 토큰과 일치 여부 확인
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        String storedToken = refreshTokenService.getRefreshToken(userId);
        if (storedToken == null) {
            throw new InvalidTokenException("Refresh token not found");
        }

        if (!storedToken.equals(refreshToken)) {
            throw new InvalidTokenException("Refresh token does not match");
        }

        return user;
    }

    /**
     * 로그아웃
     */
    public void logout(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        refreshTokenService.deleteRefreshToken(userId);
    }
}
