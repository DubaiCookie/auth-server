package com.authserver.repository;

import com.authserver.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    // 만료된 토큰 삭제 (스케줄러에서 사용)
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
