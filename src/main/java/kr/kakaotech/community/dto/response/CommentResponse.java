package kr.kakaotech.community.dto.response;

import kr.kakaotech.community.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentResponse {
    private int id;
    private String content;
    private String userId;
    private String nickname;
    private LocalDateTime createdAt;

    public static CommentResponse fromEntity(Comment comment) {
        CommentResponse response;
        if (comment.getDeleted()) {
            response = new CommentResponse(
                    comment.getId(),
                    "삭제된 댓글입니다.",
                    null,
                    null,
                    comment.getCreatedAt()
            );
        } else {
            response = new CommentResponse(
                    comment.getId(),
                    comment.getContent(),
                    comment.getUser().getId().toString(),
                    comment.getUser().getNickname().startsWith("탈퇴_") ? "탈퇴한 회원이 작성한 댓글입니다" : comment.getUser().getNickname(),
                    comment.getCreatedAt()
            );
        }

        return response;
    }
}
