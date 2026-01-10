package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostImage;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.repository.PostImageRepository;
import kr.kakaotech.community.util.ImageManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    ImageManager imageManager;
    @Mock
    ImageRepository imageRepository;
    @Mock
    PostImageRepository postImageRepository;

    @InjectMocks
    ImageService imageService;

    private MockMultipartFile createValidImageFile(String contentType, long size) {
        byte[] content = new byte[(int) size];
        return new MockMultipartFile(
                "file",           // parameter name
                "filename.jpg",         // original filename
                contentType,      // content type
                content           // content
        );
    }

    private MockMultipartFile createImageFile(String filename, String contentType, long size) {
        byte[] content = new byte[(int) size];
        return new MockMultipartFile("file", filename, contentType, content);
    }

    @Test
    @DisplayName("단일 이미지 저장 - 성공")
    void saveImage_success() {
        // given
        MockMultipartFile mockImageFile = createValidImageFile("image/jpg", 3L);
        String uploadedPath = "s3://bucket/test.jpg";

        given(imageManager.uploadImage(mockImageFile)).willReturn(uploadedPath);
        given(imageRepository.save(any(Image.class))).willReturn(new Image(uploadedPath));

        // when
        Image savedImage = imageService.saveImage(mockImageFile);

        // then
        verify(imageManager, times(1)).uploadImage(mockImageFile);
        verify(imageRepository, times(1)).save(any(Image.class));
        assertThat(savedImage.getUrl()).isEqualTo(uploadedPath);
    }

    @Test
    @DisplayName("단일 이미지 저장 - 유효성 이미지 null 실패")
    void saveImage_imageNull_fail() {
        // given
        MockMultipartFile nullMockImageFile = null;

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(nullMockImageFile))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("단일 이미지 저장 - 유효성 이미지 empty 실패")
    void saveImage_imageEmpty_fail() {
        // given
        MockMultipartFile emptyMockImageFile = createValidImageFile("image/jpg", 0L);

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(emptyMockImageFile))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("단일 이미지 저장 - 이미지 타입 null 실패")
    void saveImage_imageNullType_fail() {
        // given
        MockMultipartFile mockImageFile = createValidImageFile(null, 3L);

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(mockImageFile))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지 타입 에러");
    }

    @Test
    @DisplayName("단일 이미지 저장 - 이미지 타입 유효성 실패")
    void saveImage_imageBadType_fail() {
        // given
        MockMultipartFile mockImageFile = createValidImageFile("notImage/jpg", 3L);

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(mockImageFile))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지 타입 에러");
    }

    @Test
    @DisplayName("단일 이미지 저장 - 이미지 허용 타입 검사 실패")
    void saveImage_imageBadMimeType_fail() {
        // given
        MockMultipartFile mockImageFile = createValidImageFile("image/svg", 3L);

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(mockImageFile))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지 타입 에러");
    }

    @Test
    @DisplayName("단일 이미지 저장 - 크키 제한 실패")
    void saveImage_imageBadSize_fail() {
        // given
        MockMultipartFile mockImageFile = createValidImageFile("image/jpg", 5 * 1024 * 1024 + 1);

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(mockImageFile))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지 용량은 5MB 이하로 등록해주세요.");
    }

    @Test
    @DisplayName("게시글 이미지 저장 - 성공")
    void saveImage_withPost_success() {
        // given
        MockMultipartFile image1 = createValidImageFile("image/jpg", 3L);
        MockMultipartFile image2 = createValidImageFile("image/jpg", 3L);
        String filePath = "image/jpg";

        List<MultipartFile> images = Arrays.asList(
                image1,
                image2
        );
        Image image = new Image(filePath);
        Post post = new Post();
        PostImage postImage = new PostImage(post, image);

        given(imageManager.uploadImage(image1)).willReturn(filePath);
        given(imageManager.uploadImage(image2)).willReturn(filePath);
        given(imageRepository.save(any(Image.class))).willReturn(image);
        given(postImageRepository.save(any(PostImage.class))).willReturn(postImage);

        // when
        imageService.saveImage(images, post);

        // then
        verify(imageManager, times(1)).uploadImage(image1);
        verify(imageManager, times(1)).uploadImage(image2);
        verify(imageRepository, times(images.size())).save(any(Image.class));
        verify(postImageRepository, times(images.size())).save(any(PostImage.class));
    }

    @Test
    @DisplayName("게시글 이미지 저장 - 이미지 없음 실패")
    void saveImage_withPost_imageNull_fail() {
        // given
        Post post = new Post();

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(null, post))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("게시글 이미지 저장 - 이미지 empty 실패")
    void saveImage_withPost_imageEmpty_fail() {
        // given
        MockMultipartFile image = createValidImageFile("image/jpg", 0L);
        List<MultipartFile> images = Arrays.asList(image);
        Post post = new Post();

        // when & then
        assertThatThrownBy(() -> imageService.saveImage(images, post))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미지가 존재하지 않습니다.");
    }
}