package kr.kakaotech.community.util;

import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Component
public class LocalImageManager implements ImageManager {

    @Value("${upload-dir.image}")
    private String imageUploadPath;

    @Override
    public String uploadImage(MultipartFile image) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        String fileName = timestamp + "_" + image.getOriginalFilename();
        Path path = Path.of(imageUploadPath, fileName);

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        customTransferTo(image, path.toFile());

        return path.toString();
    }

    @Override
    public void deleteImage(File file) {

    }

    public void customTransferTo(MultipartFile image, File file) {
        try (FileOutputStream outputStream = new FileOutputStream(file);
             InputStream inputStream = image.getInputStream()) {
            byte[] bytes = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, bytesRead);
            }
        } catch (IOException e) {
            log.info("[LocalImageManager] 이미지 처리 중 에러발생 : ", e);
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }
}
