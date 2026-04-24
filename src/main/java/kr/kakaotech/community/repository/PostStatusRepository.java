package kr.kakaotech.community.repository;

import jakarta.persistence.LockModeType;
import kr.kakaotech.community.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostStatusRepository extends JpaRepository<PostStatus, Integer> {

    /**
     * 좋아요 토글 등 check-then-act 임계 구역의 진입 락.
     * PostStatus 행에 PESSIMISTIC_WRITE 락을 걸어 같은 게시글에 대한 동시 토글을 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM post_statuses p WHERE p.postId = :id")
    Optional<PostStatus> findByIdForUpdate(@Param("id") int id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE post_statuses
        SET view_count = view_count + 1
        WHERE post_id = :id
    """, nativeQuery = true)
    void incrementViewCount(@Param("id") int id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE post_statuses
        SET like_count = like_count + 1
        WHERE post_id = :id
    """, nativeQuery = true)
    void incrementLikeCount(@Param("id") int id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE post_statuses
        SET like_count = like_count - 1
        WHERE post_id = :id
    """, nativeQuery = true)
    void decrementLikeCount(@Param("id") int id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE post_statuses
        SET comment_count = comment_count + 1
        WHERE post_id = :id
    """, nativeQuery = true)
    void incrementCommentCount(@Param("id") int id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE post_statuses
        SET comment_count = GREATEST(comment_count - 1, 0)
        WHERE post_id = :id
    """, nativeQuery = true)
    void decrementCommentCount(@Param("id") int id);

}
