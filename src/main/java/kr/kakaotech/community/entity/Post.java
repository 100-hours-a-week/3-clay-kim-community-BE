package kr.kakaotech.community.entity;

import jakarta.persistence.*;
import kr.kakaotech.community.dto.request.PostModifyRequest;
import kr.kakaotech.community.dto.request.PostRegisterRequest;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;
    @Column(length = 40, nullable = false)
    private String title;
    @Column(length = 3000, nullable = false)
    private String content;
    @Column(length = 12, nullable = false)
    private String nickname;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean deleted;
    @Enumerated(EnumType.STRING)
    private PostType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> postImages = new ArrayList<>();

    public Post() {
    }

    public Post(String title, String content, PostType postType, String nickname, LocalDateTime createdAt, Boolean deleted, User user) {
        this.title = title;
        this.content = content;
        this.type = postType;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.deleted = deleted;
        this.user = user;
    }

    public static Post toEntity(PostRegisterRequest request, User user) {
        return new Post(
                request.getTitle(),
                request.getContent(),
                PostType.valueOf(request.getType().toUpperCase()),
                user.getNickname(),
                LocalDateTime.now(),
                false,
                user
        );
    }

    public void saveImage(List<PostImage> postImage) {
        this.postImages = postImage;
    }

    public void updatePost(PostModifyRequest request) {
        if (!request.getTitle().isBlank() && request.getTitle() != null) {
            this.title = request.getTitle();
        }
        if (!request.getContent().isBlank() && request.getContent() != null) {
            this.content = request.getContent();
        }
        if (!request.getType().isBlank() && request.getType() != null) {
            this.type = PostType.valueOf(request.getType().toUpperCase());
        }
    }

    public void deletePost() {
        this.deleted = true;
    }
}