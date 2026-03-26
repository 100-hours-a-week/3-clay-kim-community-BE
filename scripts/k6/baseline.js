import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ============================================================
// 종주메이트 백엔드 기준선 부하 테스트 스크립트
// 사용법:
//   로컬: k6 run --env BASE_URL=http://localhost:8080 baseline.js
//   prod:  k6 run --env BASE_URL=http://<APP_IP>:8080 --env API_PREFIX=/api baseline.js
// ============================================================

// 커스텀 메트릭
const errorRate = new Rate('error_rate');
const getPostsLatency = new Trend('get_posts_latency', true);
const getPostDetailLatency = new Trend('get_post_detail_latency', true);
const getTop10Latency = new Trend('get_top10_latency', true);
const toggleLikeLatency = new Trend('toggle_like_latency', true);

// 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = __ENV.API_PREFIX || ''; // prod 환경: /api
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || ''; // 인증이 필요한 API용

export const options = {
    scenarios: {
        baseline: {
            executor: 'constant-vus',
            vus: 50,
            duration: '5m',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'], // p95 < 2초
        error_rate: ['rate<0.05'],          // 에러율 5% 미만
    },
};

// 랜덤 postId (1 ~ 1,000,000 범위)
function randomPostId() {
    return Math.floor(Math.random() * 1000000) + 1;
}

// 공통 헤더
function getHeaders(withAuth) {
    const headers = {
        'Content-Type': 'application/json',
    };
    if (withAuth && ACCESS_TOKEN) {
        headers['Cookie'] = `accessToken=${ACCESS_TOKEN}`;
    }
    return headers;
}

export default function () {
    // 1. GET /posts (게시글 목록 조회 - 커서 페이지네이션)
    group('GET /posts', () => {
        const res = http.get(`${BASE_URL}${API_PREFIX}/posts?size=10`, {
            headers: getHeaders(false),
            tags: { name: 'GET_posts' },
        });

        const success = check(res, {
            'status is 200': (r) => r.status === 200,
        });
        errorRate.add(!success);
        getPostsLatency.add(res.timings.duration);
    });

    sleep(0.5);

    // 2. GET /posts/{postId} (게시글 상세 조회)
    group('GET /posts/{postId}', () => {
        const postId = randomPostId();
        const res = http.get(`${BASE_URL}${API_PREFIX}/posts/${postId}`, {
            headers: getHeaders(false),
            tags: { name: 'GET_post_detail' },
        });

        const success = check(res, {
            'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        });
        errorRate.add(res.status >= 500);
        getPostDetailLatency.add(res.timings.duration);
    });

    sleep(0.5);

    // 3. GET /posts/top10 (인기글 Top10)
    group('GET /posts/top10', () => {
        const res = http.get(`${BASE_URL}${API_PREFIX}/posts/top10`, {
            headers: getHeaders(false),
            tags: { name: 'GET_top10' },
        });

        const success = check(res, {
            'status is 200': (r) => r.status === 200,
        });
        errorRate.add(!success);
        getTop10Latency.add(res.timings.duration);
    });

    sleep(0.5);

    // 4. POST /posts/{postId}/likes (좋아요 토글 - 인증 필요)
    if (ACCESS_TOKEN) {
        group('POST /posts/{postId}/likes', () => {
            const postId = randomPostId();
            const res = http.post(`${BASE_URL}${API_PREFIX}/posts/${postId}/likes`, null, {
                headers: getHeaders(true),
                tags: { name: 'POST_toggle_like' },
            });

            const success = check(res, {
                'status is 200 or 401': (r) => r.status === 200 || r.status === 401,
            });
            errorRate.add(res.status >= 500);
            toggleLikeLatency.add(res.timings.duration);
        });
    }

    sleep(0.5);
}

export function handleSummary(data) {
    const summary = {
        timestamp: new Date().toISOString(),
        config: {
            vus: options.scenarios.baseline.vus,
            duration: options.scenarios.baseline.duration,
            baseUrl: BASE_URL,
        },
        results: {},
    };

    // 커스텀 메트릭 요약
    const metrics = [
        { key: 'get_posts_latency', name: 'GET /posts' },
        { key: 'get_post_detail_latency', name: 'GET /posts/{postId}' },
        { key: 'get_top10_latency', name: 'GET /posts/top10' },
        { key: 'toggle_like_latency', name: 'POST /posts/{postId}/likes' },
    ];

    metrics.forEach(({ key, name }) => {
        if (data.metrics[key]) {
            const m = data.metrics[key].values;
            summary.results[name] = {
                avg: `${m.avg.toFixed(2)}ms`,
                p50: `${m['p(50)'].toFixed(2)}ms`,
                p90: `${m['p(90)'].toFixed(2)}ms`,
                p95: `${m['p(95)'].toFixed(2)}ms`,
                p99: `${m['p(99)'].toFixed(2)}ms`,
                min: `${m.min.toFixed(2)}ms`,
                max: `${m.max.toFixed(2)}ms`,
            };
        }
    });

    if (data.metrics.error_rate) {
        summary.errorRate = `${(data.metrics.error_rate.values.rate * 100).toFixed(2)}%`;
    }

    if (data.metrics.http_reqs) {
        summary.totalRequests = data.metrics.http_reqs.values.count;
        summary.rps = data.metrics.http_reqs.values.rate.toFixed(2);
    }

    // JSON 결과 파일 저장
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'results/baseline_result.json': JSON.stringify(summary, null, 2),
    };
}

// k6 기본 텍스트 요약 (import가 안되면 간단 버전)
function textSummary(data, opts) {
    let output = '\n========== 종주메이트 부하 테스트 결과 ==========\n\n';

    const metrics = [
        { key: 'get_posts_latency', name: 'GET /posts' },
        { key: 'get_post_detail_latency', name: 'GET /posts/{postId}' },
        { key: 'get_top10_latency', name: 'GET /posts/top10' },
        { key: 'toggle_like_latency', name: 'POST /posts/{postId}/likes' },
    ];

    metrics.forEach(({ key, name }) => {
        if (data.metrics[key]) {
            const m = data.metrics[key].values;
            output += `${name}:\n`;
            output += `  p50: ${m['p(50)'].toFixed(2)}ms | p95: ${m['p(95)'].toFixed(2)}ms | p99: ${m['p(99)'].toFixed(2)}ms\n`;
            output += `  avg: ${m.avg.toFixed(2)}ms | min: ${m.min.toFixed(2)}ms | max: ${m.max.toFixed(2)}ms\n\n`;
        }
    });

    if (data.metrics.error_rate) {
        output += `에러율: ${(data.metrics.error_rate.values.rate * 100).toFixed(2)}%\n`;
    }
    if (data.metrics.http_reqs) {
        output += `총 요청: ${data.metrics.http_reqs.values.count} | RPS: ${data.metrics.http_reqs.values.rate.toFixed(2)}\n`;
    }

    output += '\n================================================\n';
    return output;
}
