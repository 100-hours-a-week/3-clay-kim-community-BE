package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @Override
    @EntityGraph(attributePaths = "image")
    Page<User> findAll(Pageable pageable);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
