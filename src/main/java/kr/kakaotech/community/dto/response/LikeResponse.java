package kr.kakaotech.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeResponse {
    boolean likeStatus;
    int likeCount;
}
