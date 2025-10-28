package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceCasCadeTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Test
    void hardDeleteUser_cascade_동작확인() {
        // given
        Image image = new Image("https://s3.com/clay_profile.jpg");
        User user = new User("clay@test.kr", "123123", "clay", "USER");

        userRepository.save(user);
        UUID userId = user.getId();
        int imageId = image.getId();

        // then
        Optional<User> getUser = userRepository.findById(userId);
        Optional<Image> getImage = imageRepository.findById(imageId);

        Assertions.assertEquals(getUser.get(), user);
        Assertions.assertEquals(getImage.get(), image);

        // when
        userService.hardDeleteUser(userId.toString());

        // then
        Optional<User> deletedUser = userRepository.findById(userId);
        Optional<Image> deletedImage = imageRepository.findById(imageId);

        assertTrue(deletedUser.isEmpty());
        assertTrue(deletedImage.isEmpty());
    }

}