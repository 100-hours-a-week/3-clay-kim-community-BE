package kr.kakaotech.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.response.CourseResponse;
import kr.kakaotech.community.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/courses")
@RequiredArgsConstructor
@RestController
public class CourseController {
    private final CourseService courseService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCourse() {
        return ApiResponse.success("코스 목록 조회 성공", courseService.getCourses());
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getSubscriptions(HttpServletRequest request) {
        UUID userId = UUID.fromString(request.getAttribute("userId").toString());

        return ApiResponse.success("코스 알림받기 목록 조회 성공", courseService.getSubscribedCourses(userId));
    }

    @PostMapping("/{courseId}/subscription")
    public ResponseEntity<ApiResponse<Void>> addSubscription(@PathVariable Integer courseId, HttpServletRequest request) {
        UUID userId = UUID.fromString(request.getAttribute("userId").toString());
        boolean created = courseService.registerCourseSubscription(courseId, userId);

        if (created) {
            return ApiResponse.create("코스 알림받기 등록 성공", null);
        }

        return ApiResponse.success("이미 알림받기 등록된 코스입니다.", null);
    }

    @DeleteMapping("/{courseId}/subscription")
    public ResponseEntity<ApiResponse<Void>> deleteSubscription(@PathVariable Integer courseId, HttpServletRequest request) {
        UUID userId = UUID.fromString(request.getAttribute("userId").toString());
        boolean deleted = courseService.deleteCourseSubscription(courseId, userId);

        if (deleted) {
            return ApiResponse.success("코스 알림받기 취소 성공", null);
        }

        return ApiResponse.success("이미 알림받기 취소된 코스입니다.", null);
    }
}
