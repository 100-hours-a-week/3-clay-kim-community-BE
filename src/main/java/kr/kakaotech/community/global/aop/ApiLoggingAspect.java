package kr.kakaotech.community.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.global.monitoring.QueryCountHolder;
import kr.kakaotech.community.global.monitoring.QueryCountMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private static final int QUERY_COUNT_WARN_THRESHOLD = 10;

    private final QueryCountMetrics queryCountMetrics;

    @Around("execution(* kr.kakaotech.community.controller..*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String method = "UNKNOWN";
        String uri = "UNKNOWN";
        String uriPattern = "UNKNOWN";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            method = request.getMethod();
            uri = request.getRequestURI();
            String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            uriPattern = (pattern != null) ? pattern : uri;
        }

        String handler = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        log.info("[API Request] {} {} → {}", method, uri, handler);
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            int queryCount = QueryCountHolder.getCount();
            queryCountMetrics.record(queryCount, method, uriPattern);
            log.info("[API Response] {} {} → {}ms | queries={}", method, uri, elapsed, queryCount);
            if (queryCount >= QUERY_COUNT_WARN_THRESHOLD) {
                log.warn("[N+1 WARNING] {} {} executed {} queries (threshold={})", method, uri, queryCount, QUERY_COUNT_WARN_THRESHOLD);
            }
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            int queryCount = QueryCountHolder.getCount();
            queryCountMetrics.record(queryCount, method, uriPattern);
            log.error("[API Error] {} {} → {}ms | queries={} | {}", method, uri, elapsed, queryCount, e.getMessage());
            throw e;
        }
    }
}
