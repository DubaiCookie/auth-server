package com.authserver.service;

import com.authserver.entity.RefreshToken;
import com.authserver.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh Token 저장
     */
    @Transactional
    public void saveRefreshToken(Long userId, String token, long expirationMillis) {
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(expirationMillis * 1_000_000);

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElse(new RefreshToken());

        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(expiresAt);

        if (refreshToken.getCreatedAt() == null) {
            refreshToken.setCreatedAt(LocalDateTime.now());
        }

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Refresh Token 조회
     */
    @Transactional(readOnly = true)
    public String getRefreshToken(Long userId) {
        return refreshTokenRepository.findByUserId(userId)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(RefreshToken::getToken)
                .orElse(null);
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     */
    @Transactional
    public void deleteRefreshToken(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Refresh Token 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasRefreshToken(Long userId) {
        return refreshTokenRepository.findByUserId(userId)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    /**
     * 만료된 토큰 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
