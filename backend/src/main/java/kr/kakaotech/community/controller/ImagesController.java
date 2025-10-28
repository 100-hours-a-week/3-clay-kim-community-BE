package kr.kakaotech.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ImagesController {

    private final ImageService imageService;

    @PostMapping("/images")
    public void saveImages(@RequestParam MultipartFile image, HttpServletRequest request) {
        System.out.println(image);
        log.info(String.valueOf(request));

        imageService.saveImage(image);
    }
}
