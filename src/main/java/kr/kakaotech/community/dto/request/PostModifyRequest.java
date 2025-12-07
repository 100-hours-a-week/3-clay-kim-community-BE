package kr.kakaotech.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PostModifyRequest {
    private String title;
    private String content;
    private String type;
    private List<Integer> removeImageIds;
}