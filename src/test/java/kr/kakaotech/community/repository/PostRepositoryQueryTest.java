package kr.kakaotech.community.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@Disabled("운영 MySQL 실행계획 확인을 위한 수동 테스트")
@SpringBootTest
@ActiveProfiles("aws")
class PostRepositoryQueryTest {

    @Autowired
    PostRepository postRepository;

    @Test
    @DisplayName("Top10 쿼리 - 기간 제한 없이 전체 조회 (before)")
    void top10_noDateFilter() {
        long start = System.currentTimeMillis();
        List<Object[]> result = postRepository.findTop10PostRowsByLikeCountIndex();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("=== [전체 조회] 결과 수: " + result.size() + ", 소요 시간: " + elapsed + "ms ===");
    }

    @Test
    @DisplayName("Top10 쿼리 - 최근 2개월 (after)")
    void top10_with2MonthFilter() {
        long start = System.currentTimeMillis();
        List<Object[]> result = postRepository.findTop10PostRowsByLikeCountIndex();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("=== [2개월 제한] 결과 수: " + result.size() + ", 소요 시간: " + elapsed + "ms ===");
    }

    @Test
    @DisplayName("Top10 쿼리 - 최근 1주일")
    void top10_with1WeekFilter() {
        long start = System.currentTimeMillis();
        List<Object[]> result = postRepository.findTop10PostRowsByLikeCountIndex();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("=== [1주일 제한] 결과 수: " + result.size() + ", 소요 시간: " + elapsed + "ms ===");
    }
}
