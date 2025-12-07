package kr.kakaotech.community.dto.response;

import kr.kakaotech.community.entity.PostType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PostDetailResponse {
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private UUID userId;
    private String nickname;
    private String profileImageUrl;
    private PostType postType;
    private List<ImageResponse> images;
}
