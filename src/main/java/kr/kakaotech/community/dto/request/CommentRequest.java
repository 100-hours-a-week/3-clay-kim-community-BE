package kr.kakaotech.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentRequest {
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 500, message = "댓글은 500자 이하로 작성해주세요.")
    private String content;
}
