package kr.kakaotech.community.repository;

import kr.kakaotech.community.dto.response.PostSummaryResponse;
import kr.kakaotech.community.dto.response.PostSummaryWithImageResponse;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer> {
    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount, u.image.url, p.type
                )
                FROM posts p
                JOIN post_statuses ps ON ps.postId = p.id
                JOIN users u ON p.user = u
                WHERE p.deleted = false
                ORDER BY p.id DESC
            """)
    List<PostSummaryResponse> findTopPost(Pageable pageable);

    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount, u.image.url, p.type
                )
                FROM posts p
                JOIN post_statuses ps ON ps.post = p
                JOIN users u ON p.user = u
                WHERE p.id < :cursor AND p.deleted = false
                ORDER BY p.id DESC
            """)
    List<PostSummaryResponse> findPostByCursor(@Param("cursor") int cursor, Pageable pageable);

    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount, u.image.url, p.type
                )
                from posts p
                join post_statuses ps on ps.post = p
                join users u on p.user = u
                where p.deleted = false
                and p.createdAt >= :startDate
                order by ps.likeCount desc
            """)
    List<PostSummaryResponse> findPostByLikeCount(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount, u.image.url, p.type
                )
                from posts p
                join post_statuses ps on ps.post = p
                join users u on p.user = u
                where p.deleted = false
                AND p.type = 'completed'
                order by ps.likeCount desc
            """)
    List<PostSummaryResponse> findTop10Post(Pageable pageable);

    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount, u.image.url, p.type
                )
                from posts p
                join post_statuses ps on ps.post = p
                join users u on p.user = u
                where p.deleted = false
                AND p.nickname = :nickname
            """)
    List<PostSummaryResponse> findPostByNickname(String nickname, Pageable pageable);

    @Query("""
        SELECT p
        FROM posts p
        LEFT JOIN FETCH p.postImages pi
        LEFT JOIN FETCH pi.image
        JOIN FETCH p.user u
        JOIN FETCH u.image
        WHERE p.deleted = false
        AND p.id = :postId
    """)
    Optional<Post> findPostDetailsWithImages(int postId);

    @Query("""
    SELECT new kr.kakaotech.community.dto.response.PostSummaryWithImageResponse(
        p.id,
        p.title,
        p.nickname,
        p.createdAt,
        ps.likeCount,
        ps.commentCount,
        ps.viewCount,
        u.image.url,
        p.type,
        (
            SELECT MIN(pi2.image.url)
            FROM post_image pi2
            WHERE pi2.post = p
        )
    )
    FROM posts p
        JOIN post_statuses ps ON ps.post = p
        JOIN users u ON p.user = u
    WHERE p.deleted = false
    ORDER BY p.createdAt DESC
    """)
    List<PostSummaryWithImageResponse> findPostWithImage(Pageable pageable);


    int countPostByType(PostType type);

    int countByDeletedFalseAndType(PostType type);
}
