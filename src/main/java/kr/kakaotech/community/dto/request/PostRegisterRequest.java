package kr.kakaotech.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PostRegisterRequest {
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 26, message = "제목은 26자 이하로 작성해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    private List<String> urlList;

    @NotBlank(message = "게시글 타입을 선택해주세요.")
    private String type;
}
