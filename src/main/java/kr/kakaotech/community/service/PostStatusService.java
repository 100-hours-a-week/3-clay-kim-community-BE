package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.response.PostStatusResponse;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostStatus;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostStatusService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;
    private final PostStatusRepository postStatusRepository;

    private static final String VIEW_KEY_PREFIX = "post:view:";
    private static final String LIKE_KEY_PREFIX = "post:like:";
    private static final String COMMENT_KEY_PREFIX = "post:comment:";
    private static final String DIRTY_KEY_PREFIX = "dirty:posts";

    /**
     * 조회수 증가
     */
    public void incrementViewCount(int postId) {
        String key = VIEW_KEY_PREFIX + postId;
        // 키가 없으면 0으로 초기화
        redisTemplate.opsForValue().setIfAbsent(key, "0");
        redisTemplate.opsForValue().increment(key);
        redisTemplate.opsForSet().add(DIRTY_KEY_PREFIX, String.valueOf(postId));
    }

    /**
     * 좋아요 증가
     */
    public void incrementLikeCount(int postId) {
        String key = LIKE_KEY_PREFIX + postId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.opsForSet().add(DIRTY_KEY_PREFIX, String.valueOf(postId));
    }

    /**
     * 좋아요 감소
     */
    public void decrementLikeCount(int postId) {
        String key = LIKE_KEY_PREFIX + postId;
        redisTemplate.opsForValue().decrement(key);
        redisTemplate.opsForSet().add(DIRTY_KEY_PREFIX, String.valueOf(postId));
    }

    /**
     * 댓글 수 추가
     */
    public void incrementCommentCount(int postId) {
        String key = COMMENT_KEY_PREFIX + postId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.opsForSet().add(DIRTY_KEY_PREFIX, String.valueOf(postId));
    }

    /**
     * postStatus 읽기
     */
    @Transactional
    public PostStatusResponse getPostStatusRedis(int postId) {
        String viewKey = VIEW_KEY_PREFIX + postId;
        String likeKey = LIKE_KEY_PREFIX + postId;
        String commentKey = COMMENT_KEY_PREFIX + postId;

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // viewCount를 Redis에서 찾았는데 없으면
        // 바로 DB 검색 및 Redis 캐싱
        if (!redisTemplate.hasKey(likeKey)) {
            PostStatus postStatus = postStatusRepository.findById(postId).orElseGet(() -> {
                return postStatusRepository.save(new PostStatus(post));
            });

            // Redis 캐싱
            redisTemplate.opsForValue().set(viewKey, String.valueOf(postStatus.getViewCount()));
            redisTemplate.opsForValue().set(likeKey, String.valueOf(postStatus.getLikeCount()));
            redisTemplate.opsForValue().set(commentKey, String.valueOf(postStatus.getCommentCount()));

            return new PostStatusResponse(
                    postStatus.getViewCount()
            );
        }

        // Redis 바로 검색
        int viewCount = parseInteger(redisTemplate.opsForValue().get(viewKey));
        int likeCount = parseInteger(redisTemplate.opsForValue().get(likeKey));
        int commentCount = parseInteger(redisTemplate.opsForValue().get(commentKey));

        return new PostStatusResponse(viewCount);
    }

//    @Scheduled(fixedDelay = 6000000) // 1분마다
    public void syncToDatabase() {
        log.info("스케쥴러 시작");
        transactionalSync();
        log.info("스케쥴러 종료");
    }

    /**
     * Redis → MySQL 동기화
     */
//    @Transactional
    public void transactionalSync() {
        Set<String> dirtyIds = redisTemplate.opsForSet().members(DIRTY_KEY_PREFIX);
        if (dirtyIds == null) return;

        for (String dirtyId : dirtyIds) {
            int postId = Integer.parseInt(dirtyId);

            // DB에 값 누적 업데이트
            getStatusFromRedis(postId);

            // 동기화 후 Redis 초기화
            redisTemplate.delete(VIEW_KEY_PREFIX + postId);
            redisTemplate.delete(COMMENT_KEY_PREFIX + postId);
            redisTemplate.delete(LIKE_KEY_PREFIX + postId);
            redisTemplate.delete(DIRTY_KEY_PREFIX + postId);
        }
    }


    private void getStatusFromRedis(int postId) {
        String viewKey = VIEW_KEY_PREFIX + postId;
        String likeKey = LIKE_KEY_PREFIX + postId;
        String commentKey = COMMENT_KEY_PREFIX + postId;

        int viewCount = parseInteger(redisTemplate.opsForValue().get(viewKey));
        int likeCount = parseInteger(redisTemplate.opsForValue().get(likeKey));
        int commentCount = parseInteger(redisTemplate.opsForValue().get(commentKey));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        PostStatus postStatus = postStatusRepository.findById(postId).orElseGet(() -> new PostStatus(post));

        postStatus.updateCount(viewCount, likeCount, commentCount);
    }

    private int parseInteger(String value) {
        if (value == null || value.isEmpty()) return 0;
        return Integer.parseInt(value);
    }

    // TODO: 조회수 해결해보기.
    @Transactional
    public void incrementViewCountRDB(int postId) {
        postStatusRepository.incrementViewCount(postId);
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

