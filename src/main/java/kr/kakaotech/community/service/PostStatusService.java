package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.response.PostStatusResponse;
import kr.kakaotech.community.dto.response.PostTypeCountResponse;
import kr.kakaotech.community.entity.PostStatus;
import kr.kakaotech.community.entity.PostType;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Service
public class PostStatusService {
    private final PostStatusRepository postStatusRepository;
    private final PostRepository postRepository;

    // TODO: 조회수 해결해보기.
    @Transactional
    public void incrementViewCountRDB(int postId) {
        postStatusRepository.incrementViewCount(postId);
    }

    public PostTypeCountResponse getPostTypeCount(String type) {
        return new PostTypeCountResponse(postRepository.countPostByType(PostType.valueOf(type.toUpperCase())));
    }

    /**
     * 조회수 불러오기
     *
     * 다른 통계정보는 각자 들고오기 때문에 임시 삭제
     */
    public PostStatusResponse getPostStatus(int postId) {
        return new PostStatusResponse(postStatusRepository.findById(postId).map(PostStatus::getViewCount).orElse(0));
    }
}

