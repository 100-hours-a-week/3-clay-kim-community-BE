package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.PostRegisterRequest;
import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.repository.PostImageRepository;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PostServiceImageTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private PostImageRepository postImageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostService postService;

    @Test
    void post_저장시_postImage_와_image도_자동저장된다() {
        // given
        Image image = new Image("test");
        imageRepository.save(image);
        User user = new User("test@test.kr", "123123", "tester", "USER", image);
        userRepository.save(user);

        PostRegisterRequest request = new PostRegisterRequest(
                "JPA Cascade Test",
                "CascadeType.ALL 테스트",
                Arrays.asList(
                        "https://s3.com/image1.png",
                        "https://s3.com/image2.png"
                ));

        // when
        postService.registerPost(user.getId().toString(), request);

        // then
        assertThat(postRepository.count()).isEqualTo(1);
        assertThat(postImageRepository.count()).isEqualTo(2);
        assertThat(imageRepository.count()).isEqualTo(3);

        Post savedPost = postRepository.findAll().get(0);
        assertThat(savedPost.getPostImages()).hasSize(2);

        System.out.println("저장된 Post ID : " + savedPost.getId());
        savedPost.getPostImages().forEach(pi ->
                System.out.println(" - Image URL : " + pi.getImage().getUrl())
        );
    }

}
