package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.Comment;
import kr.kakaotech.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    @EntityGraph(attributePaths = "user")
    Page<Comment> findByPost(Post post, Pageable pageable);
}
