package kr.kakaotech.community.util;

import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Slf4j
@RequiredArgsConstructor
@Component
public class ApiGatewayImageManager implements ImageManager {

    private final WebClient webClient;

    @Value("${image.upload.profile-url}")
    private String profileUploadUrl;

    @Value("${image.upload.post-url}")
    private String postUploadUrl;

    @Override
    public String uploadImage(MultipartFile image) {
        return uploadTo(image, profileUploadUrl);
    }

    @Override
    public void deleteImage(File file) {

    }

    public String uploadPostImage(MultipartFile image) {
        return uploadTo(image, postUploadUrl);
    }

    private String uploadTo(MultipartFile image, String url) {
        try {
            String contentType = image.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            ImageUploadResponse response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.parseMediaType(contentType)) // image/png, image/jpeg 등
                    .bodyValue(image.getBytes())                       // raw bytes
                    .retrieve()
                    .bodyToMono(ImageUploadResponse.class)
                    .block();

            if (response == null || response.data == null || response.data.filePath == null) {
                throw new CustomException(ErrorCode.SERVER_ERROR);
            }

            return response.data.filePath;

        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }

    public static class ImageUploadResponse {
        public int status;
        public String message;
        public Data data;

        public static class Data {
            public String filePath;
        }
    }
}
