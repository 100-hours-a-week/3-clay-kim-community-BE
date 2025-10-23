package kr.kakaotech.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostStatusResponse {
    private int viewCount;
    private int likeCount;
    private int commentCount;
}
