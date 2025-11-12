package kr.kakaotech.community.repository;

import kr.kakaotech.community.dto.response.PostListDto;
import kr.kakaotech.community.dto.response.PostSummaryResponse;
import kr.kakaotech.community.dto.response.PostWithStatusDto;
import kr.kakaotech.community.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
                            ps.likeCount, ps.commentCount, ps.viewCount
                )
                FROM posts p
                JOIN post_statuses ps ON ps.postId = p.id
                WHERE p.deleted = false
                ORDER BY p.id DESC
            """)
    List<PostSummaryResponse> findTopPost(Pageable pageable);

    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount
                )
                FROM posts p
                JOIN FETCH post_statuses ps ON ps.post = p
                WHERE p.id < :cursor AND p.deleted = false
                ORDER BY p.id DESC
            """)
    List<PostSummaryResponse> findPostByCursor(@Param("cursor") int cursor, Pageable pageable);

    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount
                )
                from posts p
                join fetch post_statuses ps on ps.post = p
                where p.deleted = false
                and p.createdAt >= :startDate
                order by ps.likeCount asc
            """)
    List<PostSummaryResponse> findPostByLikeCount(@Param("startDate") LocalDateTime startDate,
                                       Pageable pageable);

    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostSummaryResponse(
                            p.id, p.title, p.nickname, p.createdAt,
                            ps.likeCount, ps.commentCount, ps.viewCount
                )
                from posts p
                join fetch post_statuses ps on ps.post = p
                where p.deleted = false
                order by ps.likeCount asc
            """)
    List<PostSummaryResponse> findTop10Post(
            Pageable pageable);

    // ========== 추가된 최적화된 조회 메서드들 ==========

    /**
     * 방법 1-2: DTO 프로젝션 - 게시글 상세 조회 (content 포함)
     */
    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostWithStatusDto(
                    p.id, p.title, p.content, p.nickname, p.createdAt, p.deleted,
                    ps.viewCount, ps.likeCount, ps.commentCount
                )
                FROM posts p
                JOIN post_statuses ps ON p.id = ps.postId
                WHERE p.id = :postId AND p.deleted = false
            """)
    Optional<PostWithStatusDto> findPostWithStatusById(@Param("postId") Integer postId);

    /**
     * 방법 2: @EntityGraph - Post 엔티티와 PostStatus를 함께 조회
     * 장점: Entity로 받을 수 있어서 수정 가능
     */
    @EntityGraph(attributePaths = {"postStatus"})
    @Query("SELECT p FROM posts p WHERE p.deleted = false ORDER BY p.id DESC")
    List<Post> findAllWithPostStatusEntity(Pageable pageable);

    /**
     * 방법 2-2: @EntityGraph - 단건 조회
     */
    @EntityGraph(attributePaths = {"postStatus"})
    @Query("SELECT p FROM posts p WHERE p.id = :postId AND p.deleted = false")
    Optional<Post> findByIdWithPostStatus(@Param("postId") Integer postId);

    /**
     * 방법 4: 커서 기반 페이징 + DTO 프로젝션
     */
    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostListDto(
                    p.id, p.title, p.nickname, p.createdAt,
                    ps.viewCount, ps.likeCount, ps.commentCount
                )
                FROM posts p
                JOIN post_statuses ps ON p.id = ps.postId
                WHERE p.id < :cursor AND p.deleted = false
                ORDER BY p.id DESC
            """)
    List<PostListDto> findPostListByCursorWithStatus(@Param("cursor") Integer cursor, Pageable pageable);

    /**
     * 방법 5: 좋아요 순 정렬 + DTO 프로젝션
     */
    @Query("""
                SELECT new kr.kakaotech.community.dto.response.PostListDto(
                    p.id, p.title, p.nickname, p.createdAt,
                    ps.viewCount, ps.likeCount, ps.commentCount
                )
                FROM posts p
                JOIN post_statuses ps ON p.id = ps.postId
                WHERE p.deleted = false AND p.createdAt >= :startDate
                ORDER BY ps.likeCount DESC
            """)
    List<PostListDto> findPostListByLikeCountWithStatus(@Param("startDate") LocalDateTime startDate, Pageable pageable);

}
