package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Integer> {
}
