package kr.kakaotech.community.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    // CORS Configuration을 Bean으로 등록
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 허용할 오리진(출처) 설정
        configuration.setAllowedOrigins(new ArrayList<>(List.of(
                "http://localhost:3000")));

        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));

        // 허용할 HTTP 헤더 설정
        configuration.setAllowedHeaders(Collections.singletonList("*"));
//        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With",
//                "Accept", "Origin", "Access-Control-Request-Method",
//                "Access-Control-Request-Headers"));
        // 자격 증명(쿠키, HTTP 인증) 허용 설정
        configuration.setAllowCredentials(true);
        // 브라우저에 노출할 헤더 설정
        configuration.setExposedHeaders(Arrays.asList("access", "refresh", "Content-Type"));
        // 프리플라이트 요청 캐시 시간(초)
        configuration.setMaxAge(3600L);


        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
