package kr.kakaotech.community.entity;

import jakarta.persistence.*;
import kr.kakaotech.community.dto.request.PostModifyRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Enumerated(EnumType.STRING)
    private PostType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostImage> postImages = new ArrayList<>();

    public void saveImage(List<PostImage> postImageList) {
        this.postImages = postImageList;
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
        //TODO : 이미지 교체 작업
    }

    public void deletePost() {
        this.deleted = true;
    }
}