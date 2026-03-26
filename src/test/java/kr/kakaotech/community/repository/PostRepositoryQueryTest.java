package kr.kakaotech.community.repository;

import kr.kakaotech.community.dto.response.PostSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("aws")
class PostRepositoryQueryTest {

    @Autowired
    PostRepository postRepository;

    @Test
    @DisplayName("Top10 쿼리 - 기간 제한 없이 전체 조회 (before)")
    void top10_noDateFilter() {
        long start = System.currentTimeMillis();
        List<PostSummaryResponse> result = postRepository.findTop10Post(
                LocalDateTime.of(2000, 1, 1, 0, 0), // 사실상 전체
                PageRequest.of(0, 10)
        );
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("=== [전체 조회] 결과 수: " + result.size() + ", 소요 시간: " + elapsed + "ms ===");
    }

    @Test
    @DisplayName("Top10 쿼리 - 최근 2개월 (after)")
    void top10_with2MonthFilter() {
        long start = System.currentTimeMillis();
        List<PostSummaryResponse> result = postRepository.findTop10Post(
                LocalDateTime.now().minusMonths(2),
                PageRequest.of(0, 10)
        );
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("=== [2개월 제한] 결과 수: " + result.size() + ", 소요 시간: " + elapsed + "ms ===");
    }

    @Test
    @DisplayName("Top10 쿼리 - 최근 1주일")
    void top10_with1WeekFilter() {
        long start = System.currentTimeMillis();
        List<PostSummaryResponse> result = postRepository.findTop10Post(
                LocalDateTime.now().minusWeeks(1),
                PageRequest.of(0, 10)
        );
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("=== [1주일 제한] 결과 수: " + result.size() + ", 소요 시간: " + elapsed + "ms ===");
    }
}
