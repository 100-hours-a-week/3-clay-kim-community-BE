package kr.kakaotech.community.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_user_id", columnList = "userId"))
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected RefreshToken() {
    }

    public RefreshToken(UUID userId, String token, int refreshTtl) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = LocalDateTime.now().plusSeconds(refreshTtl);
    }
}
