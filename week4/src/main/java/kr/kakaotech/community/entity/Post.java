package kr.kakaotech.community.entity;

import jakarta.persistence.*;
import kr.kakaotech.community.dto.request.PostRegisterRequest;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
//@Table(name = "posts")
@Entity(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(columnDefinition = "INT UNSIGNED")
    private Integer id;
    @Column(length = 26, nullable = false)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> postImages = new ArrayList<>();

    public Post() {
    }

    public Post(String title, String content, String nickname, LocalDateTime createdAt, Boolean deleted, User user) {
        this.title = title;
        this.content = content;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.deleted = deleted;
        this.user = user;
    }

    public static Post toEntity(PostRegisterRequest request, User user) {
        return new Post(
                request.getTitle(),
                request.getContent(),
                user.getNickname(),
                LocalDateTime.now(),
                false,
                user
        );
    }

    public void saveImage(PostImage postImage) {
        this.postImages.add(postImage);
        postImage.setPost(this);
    }

    public void updatePost(PostRegisterRequest request) {
        if (!request.getTitle().isBlank() && request.getTitle() != null) {
            this.title = request.getTitle();
        }
        if (!request.getContent().isBlank() && request.getContent() != null) {
            this.content = request.getContent();
        }
        //TODO : 이미지 교체 작업
    }

    public void deletePost() {
        this.deleted = true;
    }
}