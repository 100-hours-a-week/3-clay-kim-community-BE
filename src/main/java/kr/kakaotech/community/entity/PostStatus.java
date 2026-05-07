package kr.kakaotech.community.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor
@Entity(name = "post_statuses")
@Table(name = "post_statuses", indexes = {
        @Index(name = "idx_post_statuses_like_count_post_id", columnList = "like_count DESC, post_id DESC")
})
public class PostStatus {
    @Id
    private Integer postId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int viewCount;

    @ColumnDefault("0")
    @Column(nullable = false)
    private int likeCount;

    @ColumnDefault("0")
    @Column(nullable = false)
    private int commentCount;

    public PostStatus(Post post) {
        this.post = post;
//        this.postId = post.getId();
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public void updateCount(int view, int like, int count) {
        this.viewCount = view;
        this.likeCount = like;
        this.commentCount = count;
    }

    public void incrementViewCount() {
        this.viewCount ++;
    }
}
