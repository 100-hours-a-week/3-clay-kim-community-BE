package kr.kakaotech.community.integration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.global.monitoring.QueryCountHolder;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * N+1 감지용 통합 테스트 베이스.
 *
 * <p>MockMvc는 {@code Filter} 타입 빈을 필터 체인에 자동 편입한다 ({@code FilterRegistrationBean}은 제외).
 * 따라서 {@link OncePerRequestFilter}를 직접 @Bean으로 노출해야 MockMvc가 체인에 포함한다.
 *
 * <p>필터는 {@code @Order(LOWEST_PRECEDENCE)}로 가장 안쪽에 배치해, MDCFilter(HIGHEST)의 finally 블록이
 * {@link QueryCountHolder}를 리셋하기 직전에 쿼리 카운트를 응답 헤더 {@code X-Query-Count}로 내보낸다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("nplusone")
@Tag("nplusone")
@Import(NPlusOneTestSupport.QueryCountCaptureConfig.class)
public abstract class NPlusOneTestSupport {

    public static final String QUERY_COUNT_HEADER = "X-Query-Count";

    @TestConfiguration
    public static class QueryCountCaptureConfig {

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        public OncePerRequestFilter queryCountCaptureFilter() {
            return new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain)
                        throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                    response.setHeader(QUERY_COUNT_HEADER, String.valueOf(QueryCountHolder.getCount()));
                }
            };
        }
    }
}
