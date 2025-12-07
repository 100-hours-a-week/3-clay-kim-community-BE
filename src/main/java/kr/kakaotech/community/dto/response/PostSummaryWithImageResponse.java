package kr.kakaotech.community.dto.response;

import kr.kakaotech.community.entity.PostType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostSummaryWithImageResponse {
    private int id;
    private String title;
    private String nickname;
    private LocalDateTime createdAt;
    private int likeCount;
    private int commentCount;
    private int viewCount;
    private String imageUrl;
    private PostType postType;
    private String postImageUrl;
}
