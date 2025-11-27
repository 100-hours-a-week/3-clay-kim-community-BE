package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceCasCadeTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Test
    void hardDeleteUser_cascade_동작확인() {
        // given
        Image image = new Image("https://s3.com/clay_profile.jpg");
        User user = new User("clay@test.kr", "123123", "clay", "USER");
        user.addImage(image);  // User랑 Image 연관관계

        userRepository.save(user);  // cascade로 Image도 함께 저장됨
        UUID userId = user.getId();
        Integer imageId = image.getId();

        // 저장 확인
        Optional<User> savedUser = userRepository.findById(userId);
        Optional<Image> savedImage = imageRepository.findById(imageId);

        assertTrue(savedUser.isPresent());
        assertTrue(savedImage.isPresent());
        assertEquals(user.getEmail(), savedUser.get().getEmail());
        assertEquals(image.getUrl(), savedImage.get().getUrl());

        // when
        userRepository.delete(user);

        // then
        Optional<User> deletedUser = userRepository.findById(userId);
        Optional<Image> deletedImage = imageRepository.findById(imageId);

        assertTrue(deletedUser.isEmpty());
        assertTrue(deletedImage.isEmpty());
    }

}