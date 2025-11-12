package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostStatusRepository extends JpaRepository<PostStatus, Integer> {

    @Modifying
    @Query(value = """
        UPDATE post_statuses
        SET view_count = view_count + 1
        WHERE post_id = :id
    """, nativeQuery = true)
    void incrementViewCount(@Param("id") int id);
}
