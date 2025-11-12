package kr.kakaotech.community.entity;

import jakarta.persistence.*;
import kr.kakaotech.community.dto.request.UserUpdateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
@Entity(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id; // binary(16) 사용
    @Column(length = 50, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false, unique = true, length = 12)
    private String nickname;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean deleted = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Image image;

    public User() {
    }

    public User(String email, String password, String nickname, String role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = UserRole.valueOf(role.toUpperCase());
        this.createdAt = LocalDateTime.now();
    }

    public void updateUser(UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest.getNickname() != null && !userUpdateRequest.getNickname().isBlank()) {
            this.nickname = userUpdateRequest.getNickname();
        }
    }

    public void deleteUser() {
        this.nickname = "탈퇴한 회원";
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void addImage(Image image) {
        this.image = image;
    }
}
