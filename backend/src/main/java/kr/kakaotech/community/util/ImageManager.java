package kr.kakaotech.community.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface ImageManager {
    String uploadImage(MultipartFile image);
    void deleteImage(File file);
}
