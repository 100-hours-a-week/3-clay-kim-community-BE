package kr.kakaotech.community.dto.response;

import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostSummaryResponse {
    private int id;
    private String title;
    private String nickname;
    private LocalDateTime createdAt;
    private int likeCount;
    private int commentCount;
    private int viewCount;

    public static PostSummaryResponse fromEntity(Post post, PostStatus postStatus) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getNickname(),
                post.getCreatedAt(),
                postStatus.getViewCount(),
                postStatus.getLikeCount(),
                postStatus.getCommentCount()
        );
    }
}
