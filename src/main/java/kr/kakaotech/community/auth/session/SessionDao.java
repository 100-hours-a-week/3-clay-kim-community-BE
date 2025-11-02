package kr.kakaotech.community.auth.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SessionDao {
    private String sessionId;
    private String userId;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;

    public void updateLastAccessedTime() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
