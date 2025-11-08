package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {
    @Query("""
        SELECT p, ps
        FROM posts p
        JOIN FETCH post_statuses ps ON ps.post = p
        WHERE p.deleted = false
        ORDER BY p.id DESC
    """)
    List<Object[]> findTopPost(Pageable pageable);

    @Query("""
        SELECT p, ps
        FROM posts p
        JOIN FETCH post_statuses ps ON ps.post = p
        WHERE p.id < :cursor AND p.deleted = false
        ORDER BY p.id DESC
    """)
    List<Object[]> findPostByCursor(@Param("cursor") int cursor, Pageable pageable);

    @Query("""
        SELECT p, ps
        from posts p
        join fetch post_statuses ps on ps.post = p
        where p.deleted = false
        and p.createdAt >= :startDate
        order by ps.likeCount desc
    """)
    List<Object[]> findPostByLikeCount(@Param("startDate") LocalDateTime startDate,
                                       Pageable pageable);

    @Query("""
        SELECT p, ps
        from posts p
        join fetch post_statuses ps on ps.post = p
        where p.deleted = false
        order by ps.likeCount desc
    """)
    List<Object[]> findTop10Post(Pageable pageable);

}
