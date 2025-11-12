package kr.kakaotech.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostStatusResponse {
    private int viewCount;
    /**
     * 통계 정보를 각자 들고오기에 임시 삭제합니다.
    private int likeCount;
    private int commentCount;
     */
}
