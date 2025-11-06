package kr.kakaotech.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ImagesController {

    private final ImageService imageService;

    @PostMapping("/images")
    public void saveImages(@RequestPart List<MultipartFile> images, HttpServletRequest request) {
        System.out.println(images);
        log.info(String.valueOf(request));

        imageService.saveImage(images, null);
    }
}
