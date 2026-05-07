package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class Top10RankingService {

    private static final String TOP10_RANKING_KEY = "posts:top10:ranking";
    private static final double POST_ID_TIE_BREAKER_UNIT = 10_000_000D;

    private final StringRedisTemplate stringRedisTemplate;

    public List<Integer> getTop10PostIds() {
        try {
            Set<String> postIds = stringRedisTemplate.opsForZSet()
                    .reverseRange(TOP10_RANKING_KEY, 0, 9);

            if (postIds == null || postIds.isEmpty()) {
                return List.of();
            }

            return postIds.stream()
                    .map(Integer::parseInt)
                    .toList();
        } catch (RedisConnectionFailureException | RedisSystemException | NumberFormatException e) {
            log.warn("Top10 Redis Sorted Set 조회 실패 - DB fallback. reason={}", e.getMessage());
            return List.of();
        }
    }

    public void syncScore(int postId, int likeCount) {
        try {
            stringRedisTemplate.opsForZSet()
                    .add(TOP10_RANKING_KEY, String.valueOf(postId), buildScore(postId, likeCount));
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            log.warn("Top10 Redis Sorted Set score 갱신 실패. postId={}, reason={}", postId, e.getMessage());
        }
    }

    public void seed(List<PostSummaryResponse> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        posts.forEach(post -> syncScore(post.getId(), post.getLikeCount()));
    }

    public void remove(int postId) {
        try {
            stringRedisTemplate.opsForZSet()
                    .remove(TOP10_RANKING_KEY, String.valueOf(postId));
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            log.warn("Top10 Redis Sorted Set 삭제 실패. postId={}, reason={}", postId, e.getMessage());
        }
    }

    private double buildScore(int postId, int likeCount) {
        return likeCount * POST_ID_TIE_BREAKER_UNIT + postId;
    }
}
