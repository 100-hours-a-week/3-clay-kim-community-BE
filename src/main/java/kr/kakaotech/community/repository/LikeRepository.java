package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUser_IdAndPost_Id(UUID userId, Integer postId);

    int countByPost_Id(Integer postId);
}
