package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.ReportRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseReportCommandService {
    private final CourseReportService courseReportService;

    @Transactional
    public void registerReport(Integer courseId, ReportRegisterRequest request, UUID userId) {
        courseReportService.registerReport(courseId, request, userId);
    }
}
