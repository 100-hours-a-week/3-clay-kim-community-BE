package kr.kakaotech.community.dto.response;

import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostStatus;
import kr.kakaotech.community.entity.PostType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    private String imageUrl;
    private PostType postType;

    private static PostSummaryResponse fromEntity(Post post, PostStatus postStatus, String userImageUrl) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getNickname(),
                post.getCreatedAt(),
                postStatus.getViewCount(),
                postStatus.getLikeCount(),
                postStatus.getCommentCount(),
                userImageUrl,
                post.getType()
        );
    }

    public static List<PostSummaryResponse> fromJoinedList(List<Object[]> resultList, String userImageUrl) {
        return resultList.stream()
                .map(result -> {
                    Post post = (Post) result[0];
                    PostStatus postStatus = (PostStatus) result[1];
                    return PostSummaryResponse.fromEntity(post, postStatus, userImageUrl);
                })
                .toList();
    }
}
