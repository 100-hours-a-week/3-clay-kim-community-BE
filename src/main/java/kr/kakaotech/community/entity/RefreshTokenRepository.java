package kr.kakaotech.community.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    void deleteByUserId(UUID userId);

    Optional<RefreshToken> findByUserId(UUID userId);
}
