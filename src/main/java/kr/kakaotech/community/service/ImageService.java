package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.response.ImageStatusResponse;
import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostImage;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.repository.PostImageRepository;
import kr.kakaotech.community.util.ImageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageService {

    private final ImageManager imageManager;
    private final ImageRepository imageRepository;
    private final PostImageRepository postImageRepository;

    // 최대 용량
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    // 허용 MIME 타입
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpg",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    /**
     * 단일 이미지 저장
     */
    public Image saveImage(MultipartFile image) {
        validateImage(image);

        // 이미지 업로드
        // S3 업로드 (Lambda 경유)
        String filePath = imageManager.uploadImage(image);

        // 이미지 저장
        Image imageEntity = new Image(filePath);

        return imageRepository.save(imageEntity);
    }

    /**
     * 게시글 이미지 여러 개 저장
     */
    public List<PostImage> saveImage(List<MultipartFile> images, Post post) {
        if (images == null || images.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_IMAGE);
        }

        List<PostImage> postImages = new ArrayList<>();
        for (MultipartFile image : images) {
            validateImage(image);

            // 이미지 업로드
            String filePath = imageManager.uploadImage(image);
            // 이미지 DB 저장
            Image imageEntity = imageRepository.save(new Image(filePath));
            PostImage postImageEntity = postImageRepository.save(new PostImage(post, imageEntity));

            postImages.add(postImageEntity);
        }

        return postImages;
    }

    /**
     * 삭제
     */
    public void deleteImage(Image image) {
        // S3 삭제 + DB 삭제 로직
    }

    /**
     * 이미지 유효성 검사
     */
    private void validateImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_IMAGE);
        }

        String contentType = image.getContentType();
        if (contentType == null) {
            throw new CustomException(ErrorCode.IMAGE_BAD_CONTENT_TYPE);
        }

        // "image/" prefix
        if (!contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.IMAGE_BAD_CONTENT_TYPE);
        }

        // 허용된 MIME 타입인지 검사
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.IMAGE_BAD_CONTENT_TYPE);
        }

        // 크기 제한
        if (image.getSize() > MAX_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    /**
     * 기본 이미지 랜덤 제공
     */
    public Image getDefaultImage() {
        int randomId = new Random().nextInt(8) + 1;
        return imageRepository.findById(randomId).orElse(null);
    }

    /**
     * 이미지 총 개수 조회
     */
    public ImageStatusResponse getImageCount() {
        return new ImageStatusResponse(imageRepository.count());
    }
}
