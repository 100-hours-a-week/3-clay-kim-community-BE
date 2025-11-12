package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.response.LikeResponse;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostLike;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.LikeRepository;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional
    public LikeResponse toggleLike(UUID userId, int postId) {
        Optional<PostLike> optionalPostLike = likeRepository.findByUser_IdAndPost_Id(userId, postId);

        // 좋아요 취소
        if (optionalPostLike.isPresent()) {
            likeRepository.delete(optionalPostLike.get());

            return new LikeResponse(false, getLikeCount(postId));
        }

        // 좋아요 등록
        // Reference 객체 사용해서 select 문 안날아가게 함
        try {
            // getReferenceById 사용 - 성능 최적화
            User userRef = userRepository.getReferenceById(userId);
            Post postRef = postRepository.getReferenceById(postId);

            PostLike newLike = new PostLike(userRef, postRef);
            likeRepository.save(newLike);

            return new LikeResponse(true, getLikeCount(postId));

        } catch (DataIntegrityViolationException e) {
            // FK 제약조건 위반
            log.error("Invalid user or post. userId={}, postId={}", userId, postId);
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
    }

    /**
     * 좋아요 상태 가져오기
     */
    public boolean getLikeStatus(Optional<Object> optionalUserId, int postId) {
        if (optionalUserId.isEmpty()) return false;

        UUID userId = UUID.fromString(optionalUserId.get().toString());
        return likeRepository.findByUser_IdAndPost_Id(userId, postId).isPresent();
    }

    /**
     * 좋아요 갯수 세기
     */
    public int getLikeCount(int postId) {
        log.info("like count {}", likeRepository.countByPost_Id(postId));
        return likeRepository.countByPost_Id(postId);
    }
}
