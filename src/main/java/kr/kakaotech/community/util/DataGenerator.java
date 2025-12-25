package kr.kakaotech.community.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 대용량 테스트 데이터 생성기
 * application.yaml에서 data.generator.enabled=true로 설정 시 실행됩니다.
 *
 * 사용법:
 * 1. application.yaml에 추가:
 *    data:
 *      generator:
 *        enabled: true  # 데이터 생성을 원할 때만 true
 *
 * 2. 애플리케이션 실행 시 자동으로 데이터 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "data.generator.enabled", havingValue = "true")
public class DataGenerator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    // 생성할 데이터 수량 설정
    private static final int USER_COUNT = 10_000;
    private static final int POST_COUNT = 1_000_000;
    private static final int COMMENT_COUNT = 2_000_000;
    private static final int POST_LIKE_COUNT = 5_000_000;
    private static final int COMMENT_LIKE_COUNT = 1_000_000;
    private static final int POST_IMAGE_COUNT = 300_000;

    // 배치 크기
    private static final int BATCH_SIZE = 5000;

    // UUID 목록 저장 (외래키 참조용)
    private final List<byte[]> userIds = new ArrayList<>();
    private final List<Integer> postIds = new ArrayList<>();
    private final List<Integer> commentIds = new ArrayList<>();
    private final List<Integer> imageIds = new ArrayList<>();

    @Override
    public void run(String... args) {
        log.info("=== 데이터 생성 시작 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 기존 데이터 확인
            Long existingUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
            if (existingUserCount != null && existingUserCount > 0) {
                log.warn("기존 데이터가 존재합니다. 데이터 생성을 건너뜁니다. (users: {}건)", existingUserCount);
                return;
            }

            generateUserProfileImages(); // User용 프로필 이미지 먼저 생성
            generateUsers();
            generatePostImages(); // Post용 이미지 생성
            generatePosts();
            generatePostStatuses();
            generateLinkPostImages(); // Post와 Image 연결
            generateComments();
            generatePostLikes();
            generateCommentLikes();

            long endTime = System.currentTimeMillis();
            log.info("=== 데이터 생성 완료 === 소요시간: {}초", (endTime - startTime) / 1000);
        } catch (Exception e) {
            log.error("데이터 생성 중 오류 발생", e);
        }
    }

    @Transactional
    public void generateUsers() {
        log.info("User 데이터 생성 시작... ({}건)", USER_COUNT);

        // 먼저 생성된 프로필 이미지 ID 조회
        List<Integer> profileImageIds = jdbcTemplate.queryForList(
                "SELECT id FROM images WHERE url LIKE 'https://example.com/profile/%' ORDER BY id",
                Integer.class
        );

        String sql = "INSERT INTO users (id, email, password, nickname, deleted, created_at, role, image_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String encodedPassword = passwordEncoder.encode("password123");

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 1; i <= USER_COUNT; i++) {
            UUID uuid = UUID.randomUUID();
            byte[] uuidBytes = uuidToBytes(uuid);
            userIds.add(uuidBytes);

            String email = "user" + i + "@test.com";
            String nickname = "사용자" + i;
            String role = i % 100 == 0 ? "ADMIN" : "USER";
            Integer imageId = profileImageIds.get(i - 1); // i는 1부터 시작하므로 -1

            batchArgs.add(new Object[]{
                    uuidBytes,
                    email,
                    encodedPassword,
                    nickname,
                    false,
                    Timestamp.valueOf(randomDateTime()),
                    role,
                    imageId
            });

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("User 생성 진행률: {}/{}", i, USER_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        log.info("User 데이터 생성 완료!");
    }

    @Transactional
    public void generateUserProfileImages() {
        log.info("User 프로필 이미지 생성 시작... ({}건)", USER_COUNT);
        String sql = "INSERT INTO images (url, local_date_time) VALUES (?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 1; i <= USER_COUNT; i++) {
            batchArgs.add(new Object[]{
                    "https://example.com/profile/" + i + ".jpg",
                    Timestamp.valueOf(randomDateTime())
            });

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("User 프로필 이미지 생성 진행률: {}/{}", i, USER_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        log.info("User 프로필 이미지 생성 완료!");
    }

    @Transactional
    public void generatePostImages() {
        log.info("Post 이미지 생성 시작... ({}건)", POST_IMAGE_COUNT);
        String sql = "INSERT INTO images (url, local_date_time) VALUES (?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 1; i <= POST_IMAGE_COUNT; i++) {
            batchArgs.add(new Object[]{
                    "https://example.com/posts/" + i + ".jpg",
                    Timestamp.valueOf(randomDateTime())
            });

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("Post 이미지 생성 진행률: {}/{}", i, POST_IMAGE_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }

        // Post Image ID 조회 (User 프로필 이미지 이후부터)
        log.info("Post Image ID 조회 중...");
        List<Integer> ids = jdbcTemplate.queryForList(
                "SELECT id FROM images WHERE url LIKE 'https://example.com/posts/%' ORDER BY id",
                Integer.class
        );
        imageIds.addAll(ids);
        log.info("Post 이미지 생성 완료! (총 {}건)", imageIds.size());
    }

    @Transactional
    public void generatePosts() {
        log.info("Post 데이터 생성 시작... ({}건)", POST_COUNT);
        String sql = "INSERT INTO posts (title, content, nickname, created_at, deleted, type, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 1; i <= POST_COUNT; i++) {
            byte[] userId = userIds.get(random(0, userIds.size() - 1));
            String type = random(0, 1) == 0 ? "IN_PROGRESS" : "COMPLETED";

            batchArgs.add(new Object[]{
                    "게시글 제목 " + i,
                    "이것은 테스트용 게시글 내용입니다. " + i + "번째 게시글입니다.",
                    "사용자" + random(1, USER_COUNT),
                    Timestamp.valueOf(randomDateTime()),
                    false,
                    type,
                    userId
            });

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("Post 생성 진행률: {}/{}", i, POST_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }

        // Post 생성 완료 후 실제 생성된 ID 목록을 DB에서 조회
        log.info("Post ID 조회 중...");
        List<Integer> ids = jdbcTemplate.queryForList("SELECT id FROM posts ORDER BY id", Integer.class);
        postIds.addAll(ids);
        log.info("Post 데이터 생성 완료! (총 {}건)", postIds.size());
    }

    @Transactional
    public void generatePostStatuses() {
        log.info("PostStatus 데이터 생성 시작... ({}건)", POST_COUNT);
        String sql = "INSERT INTO post_statuses (post_id, view_count, like_count, comment_count) VALUES (?, ?, ?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();

        for (Integer postId : postIds) {
            int viewCount = random(0, 1000);
            int likeCount = random(0, 100);
            int commentCount = random(0, 10);

            batchArgs.add(new Object[]{
                    postId,
                    viewCount,
                    likeCount,
                    commentCount
            });

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("PostStatus 생성 진행률: {}/{}", batchCount, POST_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        log.info("PostStatus 데이터 생성 완료!");
    }

    @Transactional
    public void generateLinkPostImages() {
        log.info("PostImage 연결 시작... ({}건)", POST_IMAGE_COUNT);
        String sql = "INSERT INTO post_image (post_id, image_id) VALUES (?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();
        Set<Integer> usedPostIds = new HashSet<>();

        for (int i = 0; i < POST_IMAGE_COUNT; i++) {
            // 중복되지 않는 postId 선택 (한 게시글에 하나의 이미지)
            Integer postId;
            do {
                postId = postIds.get(random(0, postIds.size() - 1));
            } while (usedPostIds.contains(postId));
            usedPostIds.add(postId);

            Integer imageId = imageIds.get(i);

            batchArgs.add(new Object[]{
                    postId,
                    imageId
            });

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("PostImage 생성 진행률: {}/{}", batchCount, POST_IMAGE_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        log.info("PostImage 데이터 생성 완료!");
    }

    @Transactional
    public void generateComments() {
        log.info("Comment 데이터 생성 시작... ({}건)", COMMENT_COUNT);
        String sql = "INSERT INTO comments (content, created_at, deleted, user_id, post_id) VALUES (?, ?, ?, ?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 1; i <= COMMENT_COUNT; i++) {
            byte[] userId = userIds.get(random(0, userIds.size() - 1));
            Integer postId = postIds.get(random(0, postIds.size() - 1));

            batchArgs.add(new Object[]{
                    "댓글 내용 " + i,
                    Timestamp.valueOf(randomDateTime()),
                    false,
                    userId,
                    postId
            });

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("Comment 생성 진행률: {}/{}", i, COMMENT_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }

        // Comment 생성 완료 후 실제 생성된 ID 목록을 DB에서 조회
        log.info("Comment ID 조회 중...");
        List<Integer> ids = jdbcTemplate.queryForList("SELECT id FROM comments ORDER BY id", Integer.class);
        commentIds.addAll(ids);
        log.info("Comment 데이터 생성 완료! (총 {}건)", commentIds.size());
    }

    @Transactional
    public void generatePostLikes() {
        log.info("PostLike 데이터 생성 시작... ({}건)", POST_LIKE_COUNT);
        String sql = "INSERT INTO post_likes (user_id, post_id, created_at) VALUES (?, ?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> uniqueLikes = new HashSet<>();

        int created = 0;
        while (created < POST_LIKE_COUNT) {
            byte[] userId = userIds.get(random(0, userIds.size() - 1));
            Integer postId = postIds.get(random(0, postIds.size() - 1));
            String key = Arrays.toString(userId) + "-" + postId;

            // 중복 방지 (같은 사용자가 같은 게시글에 좋아요 중복 불가)
            if (uniqueLikes.contains(key)) {
                continue;
            }
            uniqueLikes.add(key);

            batchArgs.add(new Object[]{
                    userId,
                    postId,
                    Timestamp.valueOf(randomDateTime())
            });

            created++;

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("PostLike 생성 진행률: {}/{}", created, POST_LIKE_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        log.info("PostLike 데이터 생성 완료!");
    }

    @Transactional
    public void generateCommentLikes() {
        log.info("CommentLike 데이터 생성 시작... ({}건)", COMMENT_LIKE_COUNT);
        String sql = "INSERT INTO comment_like (user_id, comment_id, created_at) VALUES (?, ?, ?)";

        int batchCount = 0;
        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> uniqueLikes = new HashSet<>();

        int created = 0;
        while (created < COMMENT_LIKE_COUNT) {
            byte[] userId = userIds.get(random(0, userIds.size() - 1));
            Integer commentId = commentIds.get(random(0, commentIds.size() - 1));
            String key = Arrays.toString(userId) + "-" + commentId;

            // 중복 방지
            if (uniqueLikes.contains(key)) {
                continue;
            }
            uniqueLikes.add(key);

            batchArgs.add(new Object[]{
                    userId,
                    commentId,
                    Timestamp.valueOf(randomDateTime())
            });

            created++;

            if (++batchCount % BATCH_SIZE == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("CommentLike 생성 진행률: {}/{}", created, COMMENT_LIKE_COUNT);
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        log.info("CommentLike 데이터 생성 완료!");
    }

    // 유틸리티 메서드들

    private byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> 8 * (7 - i));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> 8 * (7 - i));
        }
        return bytes;
    }

    private int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private LocalDateTime randomDateTime() {
        // 최근 1년 내의 랜덤 날짜
        long minDay = LocalDateTime.now().minusYears(1).toEpochSecond(java.time.ZoneOffset.UTC);
        long maxDay = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC);
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        return LocalDateTime.ofEpochSecond(randomDay, 0, java.time.ZoneOffset.UTC);
    }
}
