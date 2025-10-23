package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {
//    @Query("SELECT p FROM posts p " +
//            "ORDER BY p.id DESC " +
//            "LIMIT :size")
//    List<Post> findTopPost(@Param("size") int size);
//
//    @Query("SELECT p FROM posts p " +
//            "WHERE p.id < :cursor " +
//            "ORDER BY p.id DESC " +
//            "LIMIT :size")
//    List<Post> findPostByCursor(@Param("cursor") int cursor, @Param("size") int size);

    @Query("""
        SELECT p, ps
        FROM posts p
        JOIN FETCH post_statuses ps ON ps.post = p
        ORDER BY p.id DESC
    """)
    List<Object[]> findTopPost(Pageable pageable);

    @Query("""
    SELECT p, ps
    FROM posts p
    JOIN FETCH post_statuses ps ON ps.post = p
    WHERE p.id < :cursor
    ORDER BY p.id DESC
    """)
    List<Object[]> findPostByCursor(@Param("cursor") int cursor, Pageable pageable);

}
