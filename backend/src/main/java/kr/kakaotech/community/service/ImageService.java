package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.util.ImageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageService {

    private final ImageManager imageManager;
    private final ImageRepository imageRepository;

    public Image saveImage(MultipartFile image) {
        if (image.isEmpty()) throw new CustomException(ErrorCode.NOT_FOUND_IMAGE);

        log.info("image: {}", image.getContentType());

        // 이미지 검증
        String fileContentType = image.getContentType();
        if (fileContentType.equals("image/jpeg")) {
            throw new CustomException(ErrorCode.BAD_CONTENT_TYPE);
        }

        if (image.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB 이하만 가능합니다.");
        }

        // 이미지 업로드
        String filePath = imageManager.uploadImage(image);

        // 이미지 저장
        Image imageEntity = new Image(filePath);
        return imageRepository.save(imageEntity);
    }

//    public void getImage()

    public void deleteImage(Image image) {

    }


}
