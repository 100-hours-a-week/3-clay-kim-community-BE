package kr.kakaotech.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PostRegisterRequest {
    private String title;
    private String content;
    private List<String> urlList;
    private String type;
}
